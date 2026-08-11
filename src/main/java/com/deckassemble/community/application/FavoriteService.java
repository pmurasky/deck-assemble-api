package com.deckassemble.community.application;

import com.deckassemble.community.domain.DeckFavorite;
import com.deckassemble.community.domain.DeckFavoriteRepository;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.publishing.DeckPublishingService;
import com.deckassemble.decks.application.publishing.DeckVisibilityPolicy;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
    private final DeckRepository deckRepository;
    private final DeckVisibilityPolicy visibilityPolicy;

    public FavoriteService(
            DeckAccessGuard deckAccessGuard,
            DeckFavoriteRepository favoriteRepository,
            DeckPublishingService deckPublishingService,
            DeckRepository deckRepository,
            DeckVisibilityPolicy visibilityPolicy) {
        this.deckAccessGuard = deckAccessGuard;
        this.favoriteRepository = favoriteRepository;
        this.deckPublishingService = deckPublishingService;
        this.deckRepository = deckRepository;
        this.visibilityPolicy = visibilityPolicy;
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
        var page = favoriteRepository.findByProfileIdOrderByCreatedAtDesc(profileId, pageable);
        List<Long> deckIds = page.map(DeckFavorite::getDeckId).toList();
        List<Deck> decks = visibleDecks(deckIds);
        Set<Long> ids = decks.stream().map(Deck::getId).collect(Collectors.toSet());
        Map<Long, Long> counts = favoriteCounts(ids);
        var items =
                decks.stream()
                        .map(
                                deck ->
                                        new DeckDiscoveryService.Item(
                                                deck, counts.getOrDefault(deck.getId(), 0L), true))
                        .toList();
        return new PageImpl<>(items, pageable, items.size());
    }

    private DeckFavorite saveFavorite(long profileId, long deckId) {
        return favoriteRepository.save(new DeckFavorite(profileId, deckId));
    }

    private List<Deck> visibleDecks(List<Long> deckIds) {
        Map<Long, Deck> decks =
                deckRepository.findAllById(deckIds).stream()
                        .collect(Collectors.toMap(Deck::getId, Function.identity()));
        return deckIds.stream()
                .map(decks::get)
                .filter(
                        deck ->
                                deck != null
                                        && visibilityPolicy.isSharedViewAllowed(
                                                deck.getVisibility()))
                .toList();
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
