package com.deckassemble.decks.application.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit-level coverage for slug generation (which the brief-listed integration test can't force a
 * real collision for — SecureRandom output can't be seeded from a black-box test) and for
 * publish-pinning's choice between pinned-snapshot and live-deck content, which is easiest to
 * assert precisely with a mocked DeckRevisionService rather than a full revision history fixture.
 */
@ExtendWith(MockitoExtension.class)
class DeckPublishingServiceTest {

    @Mock private DeckRepository deckRepository;
    @Mock private DeckAccessGuard deckAccessGuard;
    @Mock private DeckRevisionService deckRevisionService;

    private DeckPublishingService service;

    @BeforeEach
    void setUp() {
        service =
                new DeckPublishingService(
                        deckRepository,
                        deckAccessGuard,
                        new DeckVisibilityPolicy(),
                        deckRevisionService);
    }

    @Test
    void shouldAssignAUrlSafeSlugWhenPublishingForTheFirstTime() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        when(deckAccessGuard.ownedLocked(42L)).thenReturn(deck);
        when(deckRepository.existsByShareSlug(anyString())).thenReturn(false);
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = service.updateVisibility(42L, DeckVisibility.UNLISTED);

        assertThat(result.getShareSlug()).isNotBlank();
        assertThat(result.getShareSlug()).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void shouldRetryOnSlugCollisionAndEventuallySucceed() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        when(deckAccessGuard.ownedLocked(42L)).thenReturn(deck);
        // First two candidates "collide", third is free.
        when(deckRepository.existsByShareSlug(anyString())).thenReturn(true, true, false);
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = service.updateVisibility(42L, DeckVisibility.PUBLIC);

        assertThat(result.getShareSlug()).isNotBlank();
        verify(deckRepository, times(3)).existsByShareSlug(anyString());
    }

    @Test
    void shouldGiveUpAfterFiveCollisionsRatherThanLoopForever() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        when(deckAccessGuard.ownedLocked(42L)).thenReturn(deck);
        when(deckRepository.existsByShareSlug(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.updateVisibility(42L, DeckVisibility.PUBLIC))
                .isInstanceOf(IllegalStateException.class);
        verify(deckRepository, times(5)).existsByShareSlug(anyString());
        verify(deckRepository, never()).save(any(Deck.class));
    }

    @Test
    void shouldNotGenerateASlugWhenVisibilityStaysPrivate() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        when(deckAccessGuard.ownedLocked(42L)).thenReturn(deck);
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = service.updateVisibility(42L, DeckVisibility.PRIVATE);

        assertThat(result.getShareSlug()).isNull();
        verify(deckRepository, never()).existsByShareSlug(anyString());
    }

    @Test
    void shouldKeepTheSameSlugStableAcrossVisibilityChanges() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        deck.setShareSlug("already-assigned-slug");
        when(deckAccessGuard.ownedLocked(42L)).thenReturn(deck);
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = service.updateVisibility(42L, DeckVisibility.PUBLIC);

        assertThat(result.getShareSlug()).isEqualTo("already-assigned-slug");
        verify(deckRepository, never()).existsByShareSlug(anyString());
    }

    @Test
    void shouldGenerateSlugsThatAreNotSequential() {
        Deck first = new Deck(1L, "Deck A", "COMMANDER");
        Deck second = new Deck(2L, "Deck B", "COMMANDER");
        when(deckAccessGuard.ownedLocked(1L)).thenReturn(first);
        when(deckAccessGuard.ownedLocked(2L)).thenReturn(second);
        when(deckRepository.existsByShareSlug(anyString())).thenReturn(false);
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String slugA = service.updateVisibility(1L, DeckVisibility.PUBLIC).getShareSlug();
        String slugB = service.updateVisibility(2L, DeckVisibility.PUBLIC).getShareSlug();

        // 12 URL-safe base64 chars from 9 random bytes: long and high-entropy, not a simple
        // incrementing counter like "1", "2", "deck-1".
        assertThat(slugA).isNotEqualTo(slugB).hasSize(12).isNotEqualTo("1");
        assertThat(slugB).hasSize(12).isNotEqualTo("2");
    }

    @Test
    void shouldPinTheCurrentRevisionNumberAndATimestampWhenPublishing() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        when(deckAccessGuard.ownedLocked(42L)).thenReturn(deck);
        when(deckRevisionService.currentRevisionNumber(42L)).thenReturn(3);
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = service.publish(42L);

        assertThat(result.getPublishedRevisionNumber()).isEqualTo(3);
        assertThat(result.getPublishedAt()).isNotNull();
    }

    @Test
    void shouldRejectPublishingADeckWithNoRecordedRevisions() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        when(deckAccessGuard.ownedLocked(42L)).thenReturn(deck);
        when(deckRevisionService.currentRevisionNumber(42L)).thenReturn(0);

        assertThatThrownBy(() -> service.publish(42L)).isInstanceOf(IllegalStateException.class);
        verify(deckRepository, never()).save(any(Deck.class));
    }

    @Test
    void shouldServeThePinnedSnapshotContentWhenTheDeckHasBeenPublished() {
        Deck deck = new Deck(1L, "Live Name", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", 7L);
        deck.setShareSlug("slug-1");
        deck.setVisibility(DeckVisibility.PUBLIC);
        deck.setPublishedRevisionNumber(2);
        DeckSnapshot pinned =
                new DeckSnapshot(
                        "Pinned Name",
                        "COMMANDER",
                        "Pinned description",
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        "DRAFT",
                        List.of(),
                        List.of(),
                        List.of());
        when(deckRepository.findByShareSlug("slug-1")).thenReturn(Optional.of(deck));
        when(deckRevisionService.snapshotAtForSharedAccess(7L, 2)).thenReturn(pinned);

        DeckPublishingService.SharedDeckView view = service.getShared("slug-1");

        assertThat(view.pinnedSnapshot()).isEqualTo(pinned);
        assertThat(view.deck()).isSameAs(deck);
    }

    @Test
    void shouldFallBackToLiveDeckStateWhenNeverPublished() {
        Deck deck = new Deck(1L, "Live Name", "COMMANDER");
        deck.setShareSlug("slug-2");
        deck.setVisibility(DeckVisibility.PUBLIC);
        when(deckRepository.findByShareSlug("slug-2")).thenReturn(Optional.of(deck));

        DeckPublishingService.SharedDeckView view = service.getShared("slug-2");

        assertThat(view.pinnedSnapshot()).isNull();
        verifyNoInteractions(deckRevisionService);
    }
}
