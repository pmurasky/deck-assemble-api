package com.deckassemble.community.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeckFavoriteRepository extends JpaRepository<DeckFavorite, UUID> {

    interface DeckFavoriteCount {

        Long getDeckId();

        long getFavoriteCount();
    }

    List<DeckFavorite> findByProfileId(Long profileId);

    Page<DeckFavorite> findByProfileIdOrderByCreatedAtDesc(Long profileId, Pageable pageable);

    @Query(
            """
            select deck
            from DeckFavorite favorite, Deck deck
            where favorite.profileId = :profileId
              and favorite.deckId = deck.id
              and deck.visibility <> com.deckassemble.decks.domain.publishing.DeckVisibility.PRIVATE
            order by favorite.createdAt desc, favorite.id asc
            """)
    Page<com.deckassemble.decks.domain.Deck> findVisibleFavoriteDecks(
            Long profileId, Pageable pageable);

    List<DeckFavorite> findByDeckId(Long deckId);

    Optional<DeckFavorite> findByProfileIdAndDeckId(Long profileId, Long deckId);

    boolean existsByProfileIdAndDeckId(Long profileId, Long deckId);

    @Query(
            """
            select favorite.deckId as deckId, count(favorite) as favoriteCount
            from DeckFavorite favorite
            where favorite.deckId in :deckIds
            group by favorite.deckId
            """)
    List<DeckFavoriteCount> countByDeckIds(Collection<Long> deckIds);

    @Query(
            """
            select favorite.deckId
            from DeckFavorite favorite
            where favorite.profileId = :profileId and favorite.deckId in :deckIds
            """)
    Set<Long> findDeckIdsByProfileIdAndDeckIdIn(Long profileId, Collection<Long> deckIds);
}
