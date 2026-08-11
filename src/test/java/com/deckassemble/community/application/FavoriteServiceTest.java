package com.deckassemble.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.community.domain.DeckFavorite;
import com.deckassemble.community.domain.DeckFavoriteRepository;
import com.deckassemble.community.domain.Notification.Reason;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.publishing.DeckPublishingService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private DeckAccessGuard deckAccessGuard;
    @Mock private DeckFavoriteRepository favoriteRepository;
    @Mock private DeckPublishingService deckPublishingService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private FavoriteService service;

    @BeforeEach
    void setUp() {
        service =
                new FavoriteService(
                        deckAccessGuard, favoriteRepository, deckPublishingService, eventPublisher);
    }

    @Test
    void shouldFavoriteVisibleSharedDeckOnce() {
        Deck deck = deck(7L, DeckVisibility.UNLISTED);
        when(deckPublishingService.getShared("slug"))
                .thenReturn(new DeckPublishingService.SharedDeckView(deck, null));
        when(deckAccessGuard.profileId()).thenReturn(5L);
        when(favoriteRepository.findByProfileIdAndDeckId(5L, 7L))
                .thenReturn(Optional.empty(), Optional.of(new DeckFavorite(5L, 7L)));
        when(favoriteRepository.save(any(DeckFavorite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeckFavorite created = service.favorite("slug").favorite();
        DeckFavorite retried = service.favorite("slug").favorite();

        assertThat(created.getDeckId()).isEqualTo(7L);
        assertThat(retried.getProfileId()).isEqualTo(5L);
        verify(favoriteRepository).save(any(DeckFavorite.class));
        verify(eventPublisher).publishEvent(new CommunityEvent(Reason.DECK_FAVORITED, 5L, 1L, "7"));
    }

    @Test
    void shouldUnfavoriteIdempotently() {
        Deck deck = deck(7L, DeckVisibility.PUBLIC);
        DeckFavorite favorite = new DeckFavorite(5L, 7L);
        when(deckPublishingService.getShared("slug"))
                .thenReturn(new DeckPublishingService.SharedDeckView(deck, null));
        when(deckAccessGuard.profileId()).thenReturn(5L);
        when(favoriteRepository.findByProfileIdAndDeckId(5L, 7L))
                .thenReturn(Optional.of(favorite), Optional.empty());

        service.unfavorite("slug");
        service.unfavorite("slug");

        verify(favoriteRepository).delete(favorite);
    }

    private Deck deck(long deckId, DeckVisibility visibility) {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", deckId);
        deck.setVisibility(visibility);
        deck.setShareSlug("slug");
        return deck;
    }
}
