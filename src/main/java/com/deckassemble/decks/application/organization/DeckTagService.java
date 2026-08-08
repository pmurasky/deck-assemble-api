package com.deckassemble.decks.application.organization;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.organization.DeckTag;
import com.deckassemble.decks.domain.organization.DeckTagAssignment;
import com.deckassemble.decks.domain.organization.DeckTagAssignmentRepository;
import com.deckassemble.decks.domain.organization.DeckTagRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manages a profile's reusable tags and their bulk assignment to a deck (a deck may carry any
 * number of tags). Deleting a tag drops its assignments but never the decks it was on.
 */
@Service
@Transactional
public class DeckTagService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckTagRepository deckTagRepository;
    private final DeckTagAssignmentRepository assignmentRepository;
    private final DeckRevisionService deckRevisionService;

    public DeckTagService(
            DeckAccessGuard deckAccessGuard,
            DeckTagRepository deckTagRepository,
            DeckTagAssignmentRepository assignmentRepository,
            DeckRevisionService deckRevisionService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckTagRepository = deckTagRepository;
        this.assignmentRepository = assignmentRepository;
        this.deckRevisionService = deckRevisionService;
    }

    public List<TagView> list() {
        return deckTagRepository.findByProfileIdOrderByNameAsc(deckAccessGuard.profileId()).stream()
                .map(DeckTagService::viewOf)
                .toList();
    }

    public TagView create(String name) {
        long profileId = deckAccessGuard.profileId();
        assertNameAvailable(profileId, name);
        return viewOf(deckTagRepository.save(new DeckTag(profileId, name)));
    }

    public TagView rename(long tagId, String name) {
        long profileId = deckAccessGuard.profileId();
        DeckTag tag = ownedTag(profileId, tagId);
        if (!tag.getName().equals(name)) {
            assertNameAvailable(profileId, name);
            tag.setName(name);
        }
        return viewOf(deckTagRepository.save(tag));
    }

    public void delete(long tagId) {
        long profileId = deckAccessGuard.profileId();
        DeckTag tag = ownedTag(profileId, tagId);
        assignmentRepository.deleteByTagId(tagId);
        deckTagRepository.delete(tag);
    }

    public void assignToDeck(long deckId, List<Long> tagIds) {
        deckAccessGuard.owned(deckId);
        long profileId = deckAccessGuard.profileId();
        List<Long> distinctIds = tagIds.stream().distinct().toList();
        distinctIds.forEach(tagId -> ownedTag(profileId, tagId));
        Set<Long> before = assignedTagIds(deckId);
        // Same delete-then-flush-then-insert ordering as DeckCategoryService.assignCards: a bare
        // delete-then-save would race the new rows against the old ones on (deck, tag) within one
        // Hibernate flush.
        assignmentRepository.deleteByDeckId(deckId);
        assignmentRepository.flush();
        distinctIds.forEach(
                tagId -> assignmentRepository.save(new DeckTagAssignment(deckId, tagId)));
        if (!before.equals(Set.copyOf(distinctIds))) {
            deckRevisionService.record(deckId, profileId, DeckChangeType.TAG_CHANGED);
        }
    }

    private Set<Long> assignedTagIds(long deckId) {
        return assignmentRepository.findByDeckId(deckId).stream()
                .map(DeckTagAssignment::getTagId)
                .collect(Collectors.toSet());
    }

    private DeckTag ownedTag(long profileId, long tagId) {
        return deckTagRepository
                .findByIdAndProfileId(tagId, profileId)
                .orElseThrow(DeckTagNotFoundException::new);
    }

    private void assertNameAvailable(long profileId, String name) {
        if (deckTagRepository.existsByProfileIdAndNameIgnoreCase(profileId, name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A tag named '" + name + "' already exists");
        }
    }

    private static TagView viewOf(DeckTag tag) {
        return new TagView(tag.getId(), tag.getName());
    }

    /** Read-only projection of a tag; no JPA entities escape this service. */
    public record TagView(@Nullable Long id, String name) {}
}
