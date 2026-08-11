package com.deckassemble.decks.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DeckRepository extends JpaRepository<Deck, Long>, JpaSpecificationExecutor<Deck> {

    List<Deck> findByProfileIdOrderByNameAsc(Long profileId);

    Optional<Deck> findByIdAndProfileId(Long id, Long profileId);

    List<Deck> findByFolderId(Long folderId);

    Optional<Deck> findByShareSlug(String shareSlug);

    boolean existsByShareSlug(String shareSlug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT deck FROM Deck deck WHERE deck.id = :id AND deck.profileId = :profileId")
    Optional<Deck> findLockedByIdAndProfileId(Long id, Long profileId);

    // Locks the deck row without an owner filter: collaborators (not just the owner) mutate the
    // row,
    // so DeckAccessGuard#editableLocked must be able to lock a deck it does not own. Access is
    // enforced separately by DeckCollaborationPolicy after the lock is held.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT deck FROM Deck deck WHERE deck.id = :id")
    Optional<Deck> findLockedById(Long id);

    // Deck.folderId is a plain FK with no DB constraint (see Deck's ponytail comment), so
    // deleting a folder must explicitly clear it on any decks that reference it to avoid
    // dangling ids; decks themselves are always retained.
    // clearAutomatically: a bulk update bypasses the persistence context, so any Deck already
    // loaded into it (e.g. DeckFolderService.delete's affected-deck lookup, run just before this)
    // would otherwise keep serving its stale pre-clear folderId for the rest of the transaction —
    // notably to DeckRevisionService.record's subsequent locked re-fetch of the same row.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Deck deck SET deck.folderId = null WHERE deck.folderId = :folderId")
    void clearFolderId(Long folderId);
}
