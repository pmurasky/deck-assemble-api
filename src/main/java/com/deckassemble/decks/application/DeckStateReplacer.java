package com.deckassemble.decks.application;

import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies an absolute deck state — every metadata field plus status, verbatim, {@code null}s
 * included — used only by deck-revision restore, which has a target snapshot to reproduce exactly
 * rather than a sparse set of changed fields. {@link DeckService#update} is intentionally
 * PATCH-semantic ({@code null} means "leave unchanged") for partial updates from normal callers and
 * can't express clearing a field or reversing {@code archive()}; restore needs this instead. Kept
 * as its own small collaborator rather than folded into {@code DeckService}, which is already at
 * this codebase's PMD method-count ceiling.
 */
@Service
@Transactional
public class DeckStateReplacer {

    private final DeckRepository deckRepository;
    private final DeckAccessGuard deckAccessGuard;
    private final DeckRevisionService deckRevisionService;

    public DeckStateReplacer(
            DeckRepository deckRepository,
            DeckAccessGuard deckAccessGuard,
            DeckRevisionService deckRevisionService) {
        this.deckRepository = deckRepository;
        this.deckAccessGuard = deckAccessGuard;
        this.deckRevisionService = deckRevisionService;
    }

    public void replace(long deckId, DeckUpdateRequest metadata, Deck.Status status) {
        Deck deck = deckAccessGuard.owned(deckId);
        applyFields(deck, metadata);
        deck.setStatus(status);
        Deck saved = deckRepository.save(deck);
        deckRevisionService.record(
                saved.getId(), saved.getProfileId(), DeckChangeType.METADATA_UPDATED);
    }

    private void applyFields(Deck deck, DeckUpdateRequest request) {
        deck.setName(request.name());
        deck.setFormatCode(request.formatCode());
        deck.setDescription(request.description());
        deck.setCommanderCardId(request.commanderCardId());
        deck.setSecondaryCommanderCardId(request.secondaryCommanderCardId());
        deck.setUseOwnedCardsOnly(Boolean.TRUE.equals(request.useOwnedCardsOnly()));
        deck.setBudgetLimit(request.budgetLimit());
        deck.setDesiredPowerLevel(request.desiredPowerLevel());
        deck.setPlayStyle(request.playStyle());
    }
}
