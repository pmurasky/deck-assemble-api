package com.deckassemble.community.application;

import com.deckassemble.community.domain.DeckFavorite;
import com.deckassemble.community.domain.DeckFavoriteRepository;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.publishing.DeckPublishingService;
import com.deckassemble.decks.domain.Deck;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FavoriteService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckFavoriteRepository favoriteRepository;
    private final DeckPublishingService deckPublishingService;

    public FavoriteService(
            DeckAccessGuard deckAccessGuard,
            DeckFavoriteRepository favoriteRepository,
            DeckPublishingService deckPublishingService) {
        this.deckAccessGuard = deckAccessGuard;
        this.favoriteRepository = favoriteRepository;
        this.deckPublishingService = deckPublishingService;
    }

    public FavoriteResult favorite(String slug) {
        Deck deck = deckPublishingService.getShared(slug).deck();
        long profileId = deckAccessGuard.profileId();
        return favoriteRepository
                .findByProfileIdAndDeckId(profileId, deck.getId())
                .map(favorite -> new FavoriteResult(favorite, false))
                .orElseGet(() -> new FavoriteResult(saveFavorite(profileId, deck.getId()), true));
    }

    public void unfavorite(String slug) {
        Deck deck = deckPublishingService.getShared(slug).deck();
        long profileId = deckAccessGuard.profileId();
        favoriteRepository
                .findByProfileIdAndDeckId(profileId, deck.getId())
                .ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public PageImpl<DeckDiscoveryService.Item> listFavorites(Pageable pageable) {
        long profileId = deckAccessGuard.profileId();
        var page = favoriteRepository.findVisibleFavoriteDecks(profileId, pageable);
        Set<Long> ids = page.stream().map(Deck::getId).collect(Collectors.toSet());
        Map<Long, Long> counts = favoriteCounts(ids);
        var items =
                page.stream()
                        .map(
                                deck ->
                                        new DeckDiscoveryService.Item(
                                                deck, counts.getOrDefault(deck.getId(), 0L), true))
                        .toList();
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    private DeckFavorite saveFavorite(long profileId, long deckId) {
        return favoriteRepository.save(new DeckFavorite(profileId, deckId));
    }

    private Map<Long, Long> favoriteCounts(Set<Long> deckIds) {
        if (deckIds.isEmpty()) {
            return Map.of();
        }
        return favoriteRepository.countByDeckIds(deckIds).stream()
                .collect(
                        Collectors.toMap(
                                DeckFavoriteRepository.DeckFavoriteCount::getDeckId,
                                DeckFavoriteRepository.DeckFavoriteCount::getFavoriteCount));
    }

    public record FavoriteResult(DeckFavorite favorite, boolean created) {}
}
