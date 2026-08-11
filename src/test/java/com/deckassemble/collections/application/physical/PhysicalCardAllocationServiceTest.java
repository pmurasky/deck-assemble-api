package com.deckassemble.collections.application.physical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.AllocationCommand;
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.collections.domain.physical.PhysicalCardAllocationRepository;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class PhysicalCardAllocationServiceTest extends AbstractIntegrationTest {

    @Autowired private PhysicalCardAllocationService allocationService;
    @Autowired private PhysicalCardAllocationRepository allocationRepository;
    @Autowired private DeckService deckService;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private DeckCardRepository deckCardRepository;
    @Autowired private CardCollectionRepository collectionRepository;
    @Autowired private CollectionCardRepository collectionCardRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllocateExactPrintingBeforeAlternatePrinting() {
        Fixture fixture = fixture("exact-first", 1, true, true);
        authenticate(fixture.subject());

        var result = allocationService.allocate(fixture.deckId(), request(fixture.deckCardId(), 1));

        assertThat(result.collectionCardPrintingId()).isEqualTo(fixture.exactPrintingId());
        assertThat(result.exactPrinting()).isTrue();
    }

    @Test
    void shouldAllocateAlternatePrintingWhenExactPrintingIsUnavailable() {
        Fixture fixture = fixture("alternate", 1, false, true);
        authenticate(fixture.subject());

        var result = allocationService.allocate(fixture.deckId(), request(fixture.deckCardId(), 1));

        assertThat(result.collectionCardPrintingId()).isEqualTo(fixture.alternatePrintingId());
        assertThat(result.exactPrinting()).isFalse();
    }

    @Test
    void shouldRejectOverAllocationAndRollback() {
        Fixture fixture = fixture("over", 1, true, false);
        authenticate(fixture.subject());

        assertThatThrownBy(
                        () ->
                                allocationService.allocate(
                                        fixture.deckId(), request(fixture.deckCardId(), 2)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        exception ->
                                assertThat(((ResponseStatusException) exception).getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));

        assertThat(allocationRepository.findByDeckIdOrderById(fixture.deckId())).isEmpty();
    }

    @Test
    void shouldPreventConcurrentAllocationBeyondAvailability() throws Exception {
        Fixture fixture = fixture("race", 1, true, false);
        long competingDeckId = createDeck(fixture.profileId(), "Competing Deck");
        long competingDeckCardId = addDeckCard(competingDeckId, fixture.exactPrintingId(), 1);
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        var futures =
                List.of(fixture.deckCardId(), competingDeckCardId).stream()
                        .map(
                                deckCardId ->
                                        pool.submit(
                                                () -> {
                                                    authenticate(fixture.subject());
                                                    ready.countDown();
                                                    start.await(5, TimeUnit.SECONDS);
                                                    return tryAllocate(
                                                            deckCardId == fixture.deckCardId()
                                                                    ? fixture.deckId()
                                                                    : competingDeckId,
                                                            deckCardId);
                                                }))
                        .toList();

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        var outcomes = futures.stream().map(future -> await(future)).toList();
        pool.shutdownNow();

        assertThat(outcomes).containsExactlyInAnyOrder("allocated", "conflict");
        assertThat(
                        allocationRepository.findByDeckIdOrderById(fixture.deckId()).size()
                                + allocationRepository
                                        .findByDeckIdOrderById(competingDeckId)
                                        .size())
                .isEqualTo(1);
    }

    @Test
    void shouldReleaseAllocationsWhenDeckIsDeleted() {
        Fixture fixture = fixture("delete", 1, true, false);
        authenticate(fixture.subject());
        allocationService.allocate(fixture.deckId(), request(fixture.deckCardId(), 1));

        deckService.delete(fixture.deckId());

        assertThat(allocationRepository.findByDeckIdOrderById(fixture.deckId())).isEmpty();
    }

    @Test
    void shouldReportAvailabilityWithOtherDeckAllocationsSubtracted() {
        Fixture fixture = fixture("availability", 2, true, false);
        long competingDeckId = createDeck(fixture.profileId(), "Other Deck");
        long competingDeckCardId = addDeckCard(competingDeckId, fixture.exactPrintingId(), 1);
        authenticate(fixture.subject());
        allocationService.allocate(competingDeckId, request(competingDeckCardId, 1));

        var result =
                allocationService.availabilityFor(
                        fixture.profileId(),
                        fixture.deckId(),
                        deckCardRepository.findByDeckId(fixture.deckId()).stream()
                                .map(
                                        card ->
                                                new PhysicalCardAllocationService
                                                        .DeckCardAvailabilityRequest(
                                                        card.getId(),
                                                        card.getCardPrintingId(),
                                                        card.getQuantity()))
                                .toList());

        assertThat(result)
                .singleElement()
                .satisfies(
                        availability -> {
                            assertThat(availability.availableQuantity()).isEqualTo(1);
                            assertThat(availability.missingQuantity()).isEqualTo(1);
                        });
    }

    private String tryAllocate(long deckId, long deckCardId) {
        try {
            allocationService.allocate(deckId, request(deckCardId, 1));
            return "allocated";
        } catch (ResponseStatusException exception) {
            return exception.getStatusCode().equals(HttpStatus.CONFLICT) ? "conflict" : "error";
        }
    }

    private String await(java.util.concurrent.Future<String> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private AllocationCommand request(long deckCardId, int quantity) {
        return new AllocationCommand(deckCardId, null, quantity);
    }

    private Fixture fixture(String key, int deckQuantity, boolean ownExact, boolean ownAlternate) {
        String subject = "auth0|alloc-" + key;
        long profileId = createProfile(subject);
        Card card = cardRepository.save(new Card("oracle-alloc-" + key, "Allocation Card"));
        long exactPrintingId = createPrinting(card, key + "-exact");
        long alternatePrintingId = createPrinting(card, key + "-alternate");
        long deckId = createDeck(profileId, "Deck " + key);
        long deckCardId = addDeckCard(deckId, exactPrintingId, deckQuantity);
        long collectionId = createCollection(profileId, key);
        if (ownExact) {
            addCollectionCard(collectionId, exactPrintingId, deckQuantity);
        }
        if (ownAlternate) {
            addCollectionCard(collectionId, alternatePrintingId, deckQuantity);
        }
        return new Fixture(
                subject, profileId, deckId, deckCardId, exactPrintingId, alternatePrintingId);
    }

    private long createProfile(String subject) {
        return profileRepository.save(new Profile(subject, subject)).getId();
    }

    private long createDeck(long profileId, String name) {
        return deckRepository.save(new Deck(profileId, name, "COMMANDER")).getId();
    }

    private long addDeckCard(long deckId, long printingId, int quantity) {
        return deckCardRepository
                .save(new DeckCard(deckId, printingId, quantity, DeckCard.Section.MAIN_DECK))
                .getId();
    }

    private long createCollection(long profileId, String key) {
        return collectionRepository
                .save(new CardCollection(profileId, "Collection " + key, null, true))
                .getId();
    }

    private void addCollectionCard(long collectionId, long printingId, int quantity) {
        collectionCardRepository.save(new CollectionCard(collectionId, printingId, quantity, 0));
    }

    private long createPrinting(Card card, String key) {
        MagicSet set =
                magicSetRepository.save(new MagicSet("set-" + key, setCode(key), "Set " + key));
        return printingRepository.save(new CardPrinting(card, set, "printing-" + key)).getId();
    }

    private String setCode(String key) {
        String code = "s" + Integer.toUnsignedString(key.hashCode(), 36);
        return code.length() <= 10 ? code : code.substring(0, 10);
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
            String subject,
            long profileId,
            long deckId,
            long deckCardId,
            long exactPrintingId,
            long alternatePrintingId) {}
}
