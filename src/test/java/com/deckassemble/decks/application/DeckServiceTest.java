package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardSummaryResponse;
import com.deckassemble.decks.application.collaboration.DeckCollaborationPolicy;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private DeckRepository deckRepository;
    @Mock private DeckCardRepository deckCardRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private CommanderLegalityEvaluator commanderLegalityEvaluator;
    @Mock private DeckRevisionService deckRevisionService;
    @Mock private DeckCollaborationPolicy deckCollaborationPolicy;

    private final AtomicLong nextDeckId = new AtomicLong(1L);

    @Test
    void shouldListDecksForCurrentProfile() {
        stubUser();
        when(deckRepository.findByProfileIdOrderByNameAsc(PROFILE_ID))
                .thenReturn(List.of(deck(1L), deck(2L)));
        when(deckCardRepository.findByDeckId(any(Long.class))).thenReturn(List.of());

        List<DeckResponse> result = service().list();

        assertThat(result).extracting(DeckResponse::name).containsExactly("Deck", "Deck");
    }

    @Test
    void shouldCreateDeckWithDefaults() {
        stubUser();
        stubSaveAssignsId();
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());
        DeckCreateRequest request =
                new DeckCreateRequest(
                        "New Deck", "COMMANDER", null, null, null, null, null, null, null);

        DeckResponse result = service().create(request);

        assertThat(result.name()).isEqualTo("New Deck");
        assertThat(result.useOwnedCardsOnly()).isFalse();
        assertThat(result.status()).isEqualTo("DRAFT");
    }

    @Test
    void shouldRecordCreatedRevisionOnCreate() {
        stubUser();
        stubSaveAssignsId();
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());
        DeckCreateRequest request =
                new DeckCreateRequest(
                        "New Deck", "COMMANDER", null, null, null, null, null, null, null);

        DeckResponse result = service().create(request);

        verify(deckRevisionService).record(result.id(), PROFILE_ID, DeckChangeType.CREATED);
    }

    @Test
    void shouldThrowWhenDeckNotOwned() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getById(1L)).isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldDelegateLegalityToEvaluator() {
        stubUser();
        Deck deck = deck(1L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        List<DeckCard> cards = List.of(new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK));
        when(deckCardRepository.findByDeckId(1L)).thenReturn(cards);
        DeckLegalityResponse expected = new DeckLegalityResponse(true, List.of());
        when(commanderLegalityEvaluator.evaluate(deck, cards)).thenReturn(expected);

        assertThat(service().legality(1L)).isEqualTo(expected);
    }

    @Test
    void shouldApplyOnlyProvidedFieldsOnUpdate() {
        stubUser();
        Deck deck = deck(1L);
        stubEditableLocked(deck);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());
        DeckUpdateRequest request =
                new DeckUpdateRequest(
                        "Renamed", null, null, null, null, null, null, null, null, null);

        DeckResponse result = service().update(1L, request);

        assertThat(result.name()).isEqualTo("Renamed");
        assertThat(result.formatCode()).isEqualTo("COMMANDER");
    }

    @Test
    void shouldRecordMetadataUpdatedRevisionWhenNonCommanderFieldChanges() {
        stubUser();
        Deck deck = deck(1L);
        stubEditableLocked(deck);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());
        DeckUpdateRequest request =
                new DeckUpdateRequest(
                        "Renamed", null, null, null, null, null, null, null, null, null);

        service().update(1L, request);

        verify(deckRevisionService).record(deck, PROFILE_ID, DeckChangeType.METADATA_UPDATED);
    }

    @Test
    void shouldRecordCommanderChangedRevisionWhenCommanderFieldChanges() {
        stubUser();
        Deck deck = deck(1L);
        stubEditableLocked(deck);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());
        DeckUpdateRequest request =
                new DeckUpdateRequest(null, null, null, 101L, null, null, null, null, null, null);

        service().update(1L, request);

        verify(deckRevisionService).record(deck, PROFILE_ID, DeckChangeType.COMMANDER_CHANGED);
        verify(deckRevisionService, never())
                .record(deck, PROFILE_ID, DeckChangeType.METADATA_UPDATED);
    }

    @Test
    void shouldNotRecordRevisionWhenUpdateRequestChangesNothing() {
        stubUser();
        Deck deck = deck(1L);
        stubEditableLocked(deck);
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());
        DeckUpdateRequest request =
                new DeckUpdateRequest(
                        "Deck", "COMMANDER", null, null, null, null, null, null, null, null);

        service().update(1L, request);

        verify(deckRevisionService, never()).record(any(Deck.class), anyLong(), any());
    }

    @Test
    void shouldRejectUpdateWhenExpectedRevisionStale() {
        stubUser();
        Deck deck = deck(1L);
        stubEditableLocked(deck);
        org.mockito.Mockito.doThrow(
                        new com.deckassemble.decks.application.collaboration
                                .DeckRevisionConflictException(7))
                .when(deckRevisionService)
                .assertExpectedRevision(1L, 3);
        DeckUpdateRequest request =
                new DeckUpdateRequest("Renamed", null, null, null, null, null, null, null, null, 3);

        assertThatThrownBy(() -> service().update(1L, request))
                .isInstanceOf(
                        com.deckassemble.decks.application.collaboration
                                .DeckRevisionConflictException.class);
        verify(deckRepository, never()).save(any(Deck.class));
    }

    @Test
    void shouldDeleteOwnedDeck() {
        stubUser();
        Deck deck = deck(1L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));

        service().delete(1L);

        verify(deckRepository).delete(deck);
    }

    @Test
    void shouldArchiveDeck() {
        stubUser();
        Deck deck = deck(1L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());

        DeckResponse result = service().archive(1L);

        assertThat(result.status()).isEqualTo("ARCHIVED");
        verify(deckRevisionService).record(1L, PROFILE_ID, DeckChangeType.METADATA_UPDATED);
    }

    @Test
    void shouldNotRecordRevisionWhenArchivingAlreadyArchivedDeck() {
        stubUser();
        Deck deck = deck(1L);
        deck.setStatus(Deck.Status.ARCHIVED);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());

        service().archive(1L);

        verify(deckRevisionService, never()).record(anyLong(), anyLong(), any());
    }

    @Test
    void shouldDuplicateDeckWithCards() {
        stubUser();
        Deck source = deck(1L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(source));
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(
                        inv -> {
                            Deck copy = inv.getArgument(0);
                            ReflectionTestUtils.setField(copy, "id", 2L);
                            return copy;
                        });
        when(deckCardRepository.findByDeckId(1L))
                .thenReturn(
                        List.of(
                                new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK),
                                new DeckCard(1L, 11L, 4, DeckCard.Section.SIDEBOARD)));
        when(deckCardRepository.findByDeckId(2L)).thenReturn(List.of());

        DeckResponse result = service().duplicate(1L);

        assertThat(result.name()).isEqualTo("Deck (Copy)");
        ArgumentCaptor<DeckCard> saved = ArgumentCaptor.forClass(DeckCard.class);
        verify(deckCardRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .allSatisfy(card -> assertThat(card.getDeckId()).isEqualTo(2L));
        verify(deckRevisionService).record(2L, PROFILE_ID, DeckChangeType.CREATED);
    }

    @Test
    void shouldRejectUnauthenticatedUser() {
        when(currentUser.subject()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().list()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldEmbedCommanderCardInDeckResponse() {
        stubUser();
        Deck deck = deck(1L);
        deck.setCommanderCardId(101L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of());
        var commander = mock(CardSummaryResponse.class);
        when(cardCatalogService.getLatestPrintingIdByCardIds(List.of(101L)))
                .thenReturn(Map.of(101L, 201L));
        when(cardCatalogService.getSummaryByPrintingId(201L)).thenReturn(commander);

        DeckResponse result = service().getById(1L);

        assertThat(result.commander()).isEqualTo(commander);
    }

    @Test
    void shouldReturnNullCommanderWhenPrintingMissing() {
        stubUser();
        Deck deck = deck(1L);
        deck.setCommanderCardId(101L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of());

        DeckResponse result = service().getById(1L);

        assertThat(result.commander()).isNull();
    }

    private DeckService service() {
        return new DeckService(
                deckRepository,
                deckCardRepository,
                new DeckAccessGuard(
                        currentUser, profileService, deckRepository, deckCollaborationPolicy),
                cardCatalogService,
                commanderLegalityEvaluator,
                deckRevisionService);
    }

    private void stubSaveAssignsId() {
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(
                        inv -> {
                            Deck saved = inv.getArgument(0);
                            ReflectionTestUtils.setField(saved, "id", nextDeckId.incrementAndGet());
                            return saved;
                        });
    }

    private void stubUser() {
        Profile profile = new Profile("sub", "User");
        ReflectionTestUtils.setField(profile, "id", PROFILE_ID);
        when(currentUser.subject()).thenReturn(Optional.of("sub"));
        when(profileService.getOrCreate("sub")).thenReturn(profile);
    }

    private void stubEditableLocked(Deck deck) {
        when(deckRepository.findLockedById(deck.getId())).thenReturn(Optional.of(deck));
        when(deckCollaborationPolicy.canEdit(deck, PROFILE_ID)).thenReturn(true);
    }

    private Deck deck(long id) {
        Deck deck = new Deck(PROFILE_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", id);
        return deck;
    }
}
