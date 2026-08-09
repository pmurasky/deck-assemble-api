package com.deckassemble.decks.application.history;

import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.organization.DeckCategory;
import com.deckassemble.decks.domain.organization.DeckCategoryRepository;
import com.deckassemble.decks.domain.organization.DeckTag;
import com.deckassemble.decks.domain.organization.DeckTagAssignment;
import com.deckassemble.decks.domain.organization.DeckTagAssignmentRepository;
import com.deckassemble.decks.domain.organization.DeckTagRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Assembles a deck's canonical {@link DeckSnapshot} — the deck's core fields plus its cards,
 * categories and tags, pulled live from their respective repositories — and (de)serializes it to
 * the JSON form stored on a {@link com.deckassemble.decks.domain.history.DeckRevision}. Split out
 * of {@link DeckRevisionService} so that service can depend on one collaborator instead of holding
 * every snapshot-fragment repository itself.
 */
@Component
public class DeckSnapshotBuilder {

    private final DeckCardRepository deckCardRepository;
    private final DeckCategoryRepository deckCategoryRepository;
    private final DeckTagAssignmentRepository deckTagAssignmentRepository;
    private final DeckTagRepository deckTagRepository;
    private final ObjectMapper objectMapper;

    public DeckSnapshotBuilder(
            DeckCardRepository deckCardRepository,
            DeckCategoryRepository deckCategoryRepository,
            DeckTagAssignmentRepository deckTagAssignmentRepository,
            DeckTagRepository deckTagRepository,
            ObjectMapper objectMapper) {
        this.deckCardRepository = deckCardRepository;
        this.deckCategoryRepository = deckCategoryRepository;
        this.deckTagAssignmentRepository = deckTagAssignmentRepository;
        this.deckTagRepository = deckTagRepository;
        this.objectMapper = objectMapper;
    }

    /** Builds the deck's current canonical snapshot and serializes it to JSON for storage. */
    public String toJson(Deck deck) {
        return objectMapper.writeValueAsString(build(deck));
    }

    /** Deserializes a previously stored snapshot JSON string back into a {@link DeckSnapshot}. */
    public DeckSnapshot fromJson(String json) {
        return objectMapper.readValue(json, DeckSnapshot.class);
    }

    private DeckSnapshot build(Deck deck) {
        return new DeckSnapshot(
                deck.getName(),
                deck.getFormatCode(),
                deck.getDescription(),
                deck.getCommanderCardId(),
                deck.getSecondaryCommanderCardId(),
                deck.getFolderId(),
                deck.isUseOwnedCardsOnly(),
                deck.getBudgetLimit(),
                deck.getDesiredPowerLevel(),
                deck.getPlayStyle(),
                deck.getStatus().name(),
                cardEntries(deck.getId()),
                categoryNames(deck.getId()),
                tagNames(deck.getId()));
    }

    private List<DeckSnapshot.CardEntry> cardEntries(Long deckId) {
        return deckCardRepository.findByDeckId(deckId).stream()
                .sorted(Comparator.comparing(DeckCard::getId))
                .map(
                        card ->
                                new DeckSnapshot.CardEntry(
                                        card.getCardPrintingId(),
                                        card.getQuantity(),
                                        card.getDeckSection().name(),
                                        card.getOwnershipStatus().name()))
                .toList();
    }

    private List<String> categoryNames(Long deckId) {
        return deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(deckId).stream()
                .map(DeckCategory::getName)
                .toList();
    }

    private List<String> tagNames(Long deckId) {
        List<Long> tagIds =
                deckTagAssignmentRepository.findByDeckId(deckId).stream()
                        .map(DeckTagAssignment::getTagId)
                        .toList();
        return deckTagRepository.findAllById(tagIds).stream()
                .map(DeckTag::getName)
                .sorted()
                .toList();
    }
}
