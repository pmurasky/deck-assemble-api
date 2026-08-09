package com.deckassemble.decks.application.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-level coverage for slug generation, which the brief-listed integration test can't force a
 * real collision for (SecureRandom output can't be seeded from a black-box test). Mocking
 * DeckRepository#existsByShareSlug lets us simulate a collision deterministically.
 */
@ExtendWith(MockitoExtension.class)
class DeckPublishingServiceTest {

    @Mock private DeckRepository deckRepository;
    @Mock private DeckAccessGuard deckAccessGuard;

    private DeckPublishingService service;

    @BeforeEach
    void setUp() {
        service =
                new DeckPublishingService(
                        deckRepository, deckAccessGuard, new DeckVisibilityPolicy());
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
}
