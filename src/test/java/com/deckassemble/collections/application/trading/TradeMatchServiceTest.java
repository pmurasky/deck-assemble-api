package com.deckassemble.collections.application.trading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CardPriceSnapshot;
import com.deckassemble.cards.domain.CardPriceSnapshotRepository;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.AllocationCommand;
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadata;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadataRepository;
import com.deckassemble.collections.domain.physical.PhysicalCardAllocationRepository;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import com.deckassemble.collections.domain.physical.PhysicalMetadataValues;
import com.deckassemble.collections.domain.trading.TradeList;
import com.deckassemble.collections.domain.trading.TradeListItem;
import com.deckassemble.collections.domain.trading.TradeListItemRepository;
import com.deckassemble.collections.domain.trading.TradeListRepository;
import com.deckassemble.collections.domain.trading.TradeListType;
import com.deckassemble.collections.domain.trading.TradeListVisibility;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class TradeMatchServiceTest extends AbstractIntegrationTest {

    private static final AtomicInteger SET_SEQUENCE = new AtomicInteger();

    @Autowired private TradeMatchService matchService;
    @Autowired private PhysicalCardAllocationService allocationService;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private DeckCardRepository deckCardRepository;
    @Autowired private CardCollectionRepository collectionRepository;
    @Autowired private CollectionCardRepository collectionCardRepository;
    @Autowired private PhysicalCardAllocationRepository allocationRepository;
    @Autowired private CollectionCardPhysicalMetadataRepository metadataRepository;
    @Autowired private TradeListRepository tradeListRepository;
    @Autowired private TradeListItemRepository itemRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;
    @Autowired private CardPriceSnapshotRepository priceRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRespectAvailableQuantityAndLeaveInventoryUnchanged() {
        Fixture fixture = fixture("available");
        long collectionCardId = addCollectionCard(fixture.leftCollectionId(), fixture.exactId(), 2);
        allocateOneCopy(fixture, collectionCardId);
        long leftOffer =
                list(fixture.leftProfileId(), TradeListType.OFFERED, TradeListVisibility.PUBLIC);
        item(leftOffer, fixture.exactId(), 2, null, null, null);
        long rightWant =
                list(fixture.rightProfileId(), TradeListType.WANTED, TradeListVisibility.PUBLIC);
        item(rightWant, fixture.exactId(), 2, null, null, null);
        int allocationsBefore = allocationRepository.findAll().size();
        int quantityBefore =
                collectionCardRepository
                        .findById(collectionCardId)
                        .orElseThrow()
                        .getRegularQuantity();
        authenticate(fixture.leftSubject());

        TradeMatchService.TradeMatchView result = matchService.compare(leftOffer, rightWant);

        assertThat(result.matches())
                .extracting("fromListId", "toListId", "quantity", "availableQuantity")
                .containsExactly(tuple(leftOffer, rightWant, 1, 1));
        assertThat(
                        collectionCardRepository
                                .findById(collectionCardId)
                                .orElseThrow()
                                .getRegularQuantity())
                .isEqualTo(quantityBefore);
        assertThat(allocationRepository.findAll()).hasSize(allocationsBefore);
    }

    @Test
    void shouldPreferExactPrintingButMatchAlternateCompatiblePrinting() {
        Fixture fixture = fixture("alternate");
        addCollectionCard(fixture.leftCollectionId(), fixture.alternateId(), 1);
        long leftOffer =
                list(fixture.leftProfileId(), TradeListType.OFFERED, TradeListVisibility.PUBLIC);
        item(leftOffer, fixture.exactId(), 1, null, null, null);
        long rightWant =
                list(fixture.rightProfileId(), TradeListType.WANTED, TradeListVisibility.PUBLIC);
        item(rightWant, fixture.exactId(), 1, null, null, null);
        authenticate(fixture.leftSubject());

        TradeMatchService.TradeMatchView result = matchService.compare(leftOffer, rightWant);

        assertThat(result.matches())
                .singleElement()
                .satisfies(
                        match -> {
                            assertThat(match.matchedCollectionCardPrintingId())
                                    .isEqualTo(fixture.alternateId());
                            assertThat(match.exactPrinting()).isFalse();
                        });
    }

    @Test
    void shouldFilterMatchesByPhysicalMetadata() {
        Fixture fixture = fixture("metadata");
        long cardId = addCollectionCard(fixture.leftCollectionId(), fixture.exactId(), 1);
        metadata(cardId, CardCondition.NEAR_MINT, PhysicalFinish.FOIL, "ja");
        long leftOffer =
                list(fixture.leftProfileId(), TradeListType.OFFERED, TradeListVisibility.PUBLIC);
        item(leftOffer, fixture.exactId(), 1, CardCondition.NEAR_MINT, PhysicalFinish.FOIL, "ja");
        long rightWant =
                list(fixture.rightProfileId(), TradeListType.WANTED, TradeListVisibility.PUBLIC);
        item(
                rightWant,
                fixture.exactId(),
                1,
                CardCondition.LIGHTLY_PLAYED,
                PhysicalFinish.FOIL,
                "ja");
        authenticate(fixture.leftSubject());

        TradeMatchService.TradeMatchView result = matchService.compare(leftOffer, rightWant);

        assertThat(result.matches()).isEmpty();
    }

    @Test
    void shouldGroupValueDeltasByCurrencyAndFlagMissingPrices() {
        Fixture fixture = fixture("prices");
        addCollectionCard(fixture.leftCollectionId(), fixture.exactId(), 2);
        addCollectionCard(fixture.rightCollectionId(), fixture.rightExactId(), 1);
        price(fixture.exactId(), "1.50", null, "2.00", null);
        long leftOffer =
                list(fixture.leftProfileId(), TradeListType.OFFERED, TradeListVisibility.PUBLIC);
        item(leftOffer, fixture.exactId(), 2, null, null, null);
        long rightWant =
                list(fixture.rightProfileId(), TradeListType.WANTED, TradeListVisibility.PUBLIC);
        item(rightWant, fixture.exactId(), 2, null, null, null);
        long rightOffer =
                list(fixture.rightProfileId(), TradeListType.OFFERED, TradeListVisibility.PUBLIC);
        item(rightOffer, fixture.rightExactId(), 1, null, null, null);
        long leftWant =
                list(fixture.leftProfileId(), TradeListType.WANTED, TradeListVisibility.PUBLIC);
        item(leftWant, fixture.rightExactId(), 1, null, null, null);
        authenticate(fixture.leftSubject());

        TradeMatchService.TradeMatchView result = matchService.compare(leftOffer, rightWant);
        TradeMatchService.TradeMatchView missing = matchService.compare(rightOffer, leftWant);

        assertThat(result.valueDeltas())
                .extracting("currency", "leftToRight", "rightToLeft")
                .containsExactlyInAnyOrder(
                        tuple("eur", new BigDecimal("4.00"), BigDecimal.ZERO),
                        tuple("usd", new BigDecimal("3.00"), BigDecimal.ZERO));
        assertThat(missing.unpricedItems()).isEqualTo(1);
        assertThat(missing.valueDeltas()).isEmpty();
    }

    @Test
    void shouldReportLeftToRightValueWhenLeftListIdIsHigherThanRightListId() {
        Fixture fixture = fixture("direction");
        addCollectionCard(fixture.leftCollectionId(), fixture.exactId(), 1);
        price(fixture.exactId(), "5.00", null, null, null);
        long lowIdWant =
                list(fixture.rightProfileId(), TradeListType.WANTED, TradeListVisibility.PUBLIC);
        item(lowIdWant, fixture.exactId(), 1, null, null, null);
        long highIdOffer =
                list(fixture.leftProfileId(), TradeListType.OFFERED, TradeListVisibility.PUBLIC);
        item(highIdOffer, fixture.exactId(), 1, null, null, null);
        authenticate(fixture.leftSubject());

        TradeMatchService.TradeMatchView result = matchService.compare(highIdOffer, lowIdWant);

        assertThat(highIdOffer).isGreaterThan(lowIdWant);
        assertThat(result.valueDeltas())
                .extracting("currency", "leftToRight", "rightToLeft")
                .containsExactly(tuple("usd", new BigDecimal("5.00"), BigDecimal.ZERO));
    }

    @ParameterizedTest
    @MethodSource("visibilityCases")
    void shouldApplyTradeListVisibilityMatrix(
            TradeListVisibility visibility,
            boolean requesterOwnsRestricted,
            boolean restrictedOnLeft,
            boolean allowed) {
        Fixture fixture =
                fixture("visibility-" + visibility + requesterOwnsRestricted + restrictedOnLeft);
        long restrictedOwner = fixture.leftProfileId();
        long counterpartOwner = fixture.rightProfileId();
        long restricted = list(restrictedOwner, sideType(restrictedOnLeft), visibility);
        long counterpart =
                list(counterpartOwner, sideType(!restrictedOnLeft), TradeListVisibility.PUBLIC);
        authenticate(requesterOwnsRestricted ? fixture.leftSubject() : fixture.rightSubject());

        if (allowed) {
            TradeMatchService.TradeMatchView result =
                    compare(restrictedOnLeft, restricted, counterpart);

            assertThat(result.leftListId()).isEqualTo(restrictedOnLeft ? restricted : counterpart);
            return;
        }
        assertThatThrownBy(() -> compare(restrictedOnLeft, restricted, counterpart))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertStatus(exception, HttpStatus.NOT_FOUND));
    }

    private static Stream<Arguments> visibilityCases() {
        return Stream.of(
                Arguments.of(TradeListVisibility.PUBLIC, false, true, true),
                Arguments.of(TradeListVisibility.PUBLIC, false, false, true),
                Arguments.of(TradeListVisibility.UNLISTED, false, true, true),
                Arguments.of(TradeListVisibility.UNLISTED, false, false, true),
                Arguments.of(TradeListVisibility.PRIVATE, true, true, true),
                Arguments.of(TradeListVisibility.PRIVATE, true, false, true),
                Arguments.of(TradeListVisibility.PRIVATE, false, true, false),
                Arguments.of(TradeListVisibility.PRIVATE, false, false, false));
    }

    private TradeListType sideType(boolean leftSide) {
        return leftSide ? TradeListType.OFFERED : TradeListType.WANTED;
    }

    private TradeMatchService.TradeMatchView compare(
            boolean restrictedOnLeft, long restricted, long counterpart) {
        return restrictedOnLeft
                ? matchService.compare(restricted, counterpart)
                : matchService.compare(counterpart, restricted);
    }

    private void assertStatus(Throwable exception, HttpStatus status) {
        assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(status);
    }

    private void allocateOneCopy(Fixture fixture, long collectionCardId) {
        long deckId =
                deckRepository.save(new Deck(fixture.leftProfileId(), "Deck", "COMMANDER")).getId();
        long deckCardId =
                deckCardRepository
                        .save(
                                new DeckCard(
                                        deckId, fixture.exactId(), 1, DeckCard.Section.MAIN_DECK))
                        .getId();
        authenticate(fixture.leftSubject());
        allocationService.allocate(deckId, new AllocationCommand(deckCardId, collectionCardId, 1));
    }

    private Fixture fixture(String key) {
        String leftSubject = "auth0|trade-left-" + key;
        String rightSubject = "auth0|trade-right-" + key;
        long leftProfileId = profile(leftSubject);
        long rightProfileId = profile(rightSubject);
        Card card = cardRepository.save(new Card("oracle-trade-" + key, "Trade Card"));
        Card other = cardRepository.save(new Card("oracle-trade-other-" + key, "Other Trade Card"));
        long exactId = printing(card, key + "-exact");
        long alternateId = printing(card, key + "-alt");
        long rightExactId = printing(other, key + "-right");
        long leftCollectionId = collection(leftProfileId, key + " left");
        long rightCollectionId = collection(rightProfileId, key + " right");
        return new Fixture(
                leftSubject,
                rightSubject,
                leftProfileId,
                rightProfileId,
                leftCollectionId,
                rightCollectionId,
                exactId,
                alternateId,
                rightExactId);
    }

    private long profile(String subject) {
        return profileRepository.save(new Profile(subject, subject)).getId();
    }

    private long collection(long profileId, String name) {
        return collectionRepository.save(new CardCollection(profileId, name, null, true)).getId();
    }

    private long addCollectionCard(long collectionId, long printingId, int quantity) {
        return collectionCardRepository
                .save(new CollectionCard(collectionId, printingId, quantity, 0))
                .getId();
    }

    private long list(long profileId, TradeListType type, TradeListVisibility visibility) {
        return tradeListRepository.save(new TradeList(profileId, type, "List", visibility)).getId();
    }

    private void item(
            long listId,
            long printingId,
            int quantity,
            CardCondition condition,
            PhysicalFinish finish,
            String language) {
        itemRepository.save(
                new TradeListItem(listId, printingId, quantity, condition, finish, language));
    }

    private void metadata(
            long collectionCardId,
            CardCondition condition,
            PhysicalFinish finish,
            String language) {
        var values =
                new PhysicalMetadataValues(
                        condition, language, finish, null, null, LocalDate.now(), null, null);
        var metadata = new CollectionCardPhysicalMetadata(collectionCardId);
        metadata.update(values);
        metadataRepository.save(metadata);
    }

    private void price(long printingId, String usd, String usdFoil, String eur, String tix) {
        priceRepository.save(
                new CardPriceSnapshot(
                        printingId,
                        new CardPrice(decimal(usd), decimal(usdFoil), decimal(eur), decimal(tix)),
                        Instant.now()));
    }

    private BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private long printing(Card card, String key) {
        MagicSet set = magicSetRepository.save(new MagicSet("set-trade-" + key, setCode(), "Set"));
        return printingRepository
                .save(new CardPrinting(card, set, "printing-trade-" + key))
                .getId();
    }

    private String setCode() {
        return "tm" + SET_SEQUENCE.incrementAndGet();
    }

    private void authenticate(String subject) {
        Jwt jwt =
                Jwt.withTokenValue("token-" + subject)
                        .header("alg", "none")
                        .subject(subject)
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private record Fixture(
            String leftSubject,
            String rightSubject,
            long leftProfileId,
            long rightProfileId,
            long leftCollectionId,
            long rightCollectionId,
            long exactId,
            long alternateId,
            long rightExactId) {}
}
