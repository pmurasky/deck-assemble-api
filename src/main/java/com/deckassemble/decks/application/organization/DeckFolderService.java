package com.deckassemble.decks.application.organization;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.organization.DeckFolder;
import com.deckassemble.decks.domain.organization.DeckFolderRepository;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manages a profile's deck folders and the one-folder-per-deck assignment. Deleting a folder clears
 * the reference on any deck that had it, but never deletes the deck.
 */
@Service
@Transactional
public class DeckFolderService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckFolderRepository deckFolderRepository;
    private final DeckRepository deckRepository;
    private final DeckRevisionService deckRevisionService;

    public DeckFolderService(
            DeckAccessGuard deckAccessGuard,
            DeckFolderRepository deckFolderRepository,
            DeckRepository deckRepository,
            DeckRevisionService deckRevisionService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckFolderRepository = deckFolderRepository;
        this.deckRepository = deckRepository;
        this.deckRevisionService = deckRevisionService;
    }

    public List<FolderView> list() {
        return deckFolderRepository
                .findByProfileIdOrderByNameAsc(deckAccessGuard.profileId())
                .stream()
                .map(DeckFolderService::viewOf)
                .toList();
    }

    public FolderView create(String name) {
        long profileId = deckAccessGuard.profileId();
        assertNameAvailable(profileId, name);
        return viewOf(deckFolderRepository.save(new DeckFolder(profileId, name)));
    }

    public FolderView rename(long folderId, String name) {
        long profileId = deckAccessGuard.profileId();
        DeckFolder folder = ownedFolder(profileId, folderId);
        if (!folder.getName().equals(name)) {
            assertNameAvailable(profileId, name);
            folder.setName(name);
        }
        return viewOf(deckFolderRepository.save(folder));
    }

    public void delete(long folderId) {
        long profileId = deckAccessGuard.profileId();
        DeckFolder folder = ownedFolder(profileId, folderId);
        deckRepository.clearFolderId(folder.getId());
        deckFolderRepository.delete(folder);
    }

    public void assignToDeck(long deckId, @Nullable Long folderId) {
        Deck deck = deckAccessGuard.owned(deckId);
        if (folderId != null) {
            ownedFolder(deckAccessGuard.profileId(), folderId);
        }
        boolean changed = !Objects.equals(deck.getFolderId(), folderId);
        deck.setFolderId(folderId);
        deckRepository.save(deck);
        if (changed) {
            deckRevisionService.record(
                    deckId, deckAccessGuard.profileId(), DeckChangeType.FOLDER_CHANGED);
        }
    }

    private DeckFolder ownedFolder(long profileId, long folderId) {
        return deckFolderRepository
                .findByIdAndProfileId(folderId, profileId)
                .orElseThrow(DeckFolderNotFoundException::new);
    }

    private void assertNameAvailable(long profileId, String name) {
        if (deckFolderRepository.existsByProfileIdAndNameIgnoreCase(profileId, name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A folder named '" + name + "' already exists");
        }
    }

    private static FolderView viewOf(DeckFolder folder) {
        return new FolderView(folder.getId(), folder.getName());
    }

    /** Read-only projection of a folder; no JPA entities escape this service. */
    public record FolderView(@Nullable Long id, String name) {}
}
