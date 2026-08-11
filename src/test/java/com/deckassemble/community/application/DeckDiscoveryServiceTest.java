package com.deckassemble.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.community.domain.DeckFavoriteRepository;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DeckDiscoveryServiceTest {

    @Mock private DeckRepository deckRepository;
    @Mock private DeckFavoriteRepository favoriteRepository;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;

    private DeckDiscoveryService service;

    @BeforeEach
    void setUp() {
        service =
                new DeckDiscoveryService(
                        deckRepository,
                        favoriteRepository,
                        cardCatalogService,
                        currentUser,
                        profileService);
    }

    @Test
    void shouldBatchDecorateFavoriteCountsAndViewerFlags() {
        Deck first = deck(1L, "A");
        Deck second = deck(2L, "B");
        when(deckRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 2));
        when(favoriteRepository.countByDeckIds(Set.of(1L, 2L))).thenReturn(List.of(count(1L, 3L)));
        when(currentUser.subject()).thenReturn(Optional.of("auth|viewer"));
        Profile viewer = new Profile("auth|viewer", "viewer");
        ReflectionTestUtils.setField(viewer, "id", 5L);
        when(profileService.findBySubject("auth|viewer")).thenReturn(Optional.of(viewer));
        when(favoriteRepository.findDeckIdsByProfileIdAndDeckIdIn(any(Long.class), any(Set.class)))
                .thenReturn(Set.of(2L));

        Page<DeckDiscoveryService.Item> response =
                service.discover(emptyQuery(), PageRequest.of(0, 2));

        assertThat(response.getContent())
                .extracting(item -> item.deck().getId())
                .containsExactly(1L, 2L);
        assertThat(response.getContent())
                .extracting(DeckDiscoveryService.Item::favoriteCount)
                .containsExactly(3L, 0L);
        assertThat(response.getContent())
                .extracting(DeckDiscoveryService.Item::favoritedByViewer)
                .containsExactly(false, true);
    }

    @Test
    void shouldRejectUnsupportedSortFields() {
        assertThatThrownBy(
                        () ->
                                service.discover(
                                        emptyQuery(),
                                        PageRequest.of(0, 10)
                                                .withSort(
                                                        org.springframework.data.domain.Sort.by(
                                                                "drop table"))))
                .isInstanceOf(ResponseStatusException.class);
    }

    private DeckDiscoveryService.Query emptyQuery() {
        return new DeckDiscoveryService.Query(null, List.of(), List.of(), null, null, null, null);
    }

    private Deck deck(long id, String name) {
        Deck deck = new Deck(10L, name, "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", id);
        deck.setShareSlug("slug-" + id);
        return deck;
    }

    private DeckFavoriteRepository.DeckFavoriteCount count(long deckId, long total) {
        return new DeckFavoriteRepository.DeckFavoriteCount() {
            @Override
            public Long getDeckId() {
                return deckId;
            }

            @Override
            public long getFavoriteCount() {
                return total;
            }
        };
    }
}
