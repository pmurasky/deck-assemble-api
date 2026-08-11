package com.deckassemble.community.api;

import com.deckassemble.community.application.DeckDiscoveryService;
import com.deckassemble.decks.domain.Deck;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

public record DeckDiscoveryResponse(
        List<Item> content, long totalElements, int totalPages, int page, int size) {

    public static DeckDiscoveryResponse from(Page<DeckDiscoveryService.Item> page) {
        return new DeckDiscoveryResponse(
                page.map(Item::from).getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    public record Item(
            long deckId,
            long profileId,
            String name,
            String formatCode,
            @Nullable String description,
            @Nullable Long commanderCardId,
            @Nullable String shareSlug,
            @Nullable Instant publishedAt,
            @Nullable Instant updatedAt,
            long favoriteCount,
            boolean favoritedByViewer) {

        public static Item from(Deck deck, long favoriteCount, boolean favoritedByViewer) {
            return new Item(
                    deck.getId(),
                    deck.getProfileId(),
                    deck.getName(),
                    deck.getFormatCode(),
                    deck.getDescription(),
                    deck.getCommanderCardId(),
                    deck.getShareSlug(),
                    deck.getPublishedAt(),
                    deck.getUpdatedAt(),
                    favoriteCount,
                    favoritedByViewer);
        }

        public static Item from(DeckDiscoveryService.Item item) {
            return from(item.deck(), item.favoriteCount(), item.favoritedByViewer());
        }
    }
}
