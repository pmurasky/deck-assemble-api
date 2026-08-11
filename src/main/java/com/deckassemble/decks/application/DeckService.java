package com.deckassemble.decks.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardNotFoundException;
import com.deckassemble.cards.application.CardSummaryResponse;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeckService {

    private final DeckRepository deckRepository;
    private final DeckCardRepository deckCardRepository;
    private final DeckAccessGuard deckAccessGuard;
    private final CardCatalogService cardCatalogService;
    private final CommanderLegalityEvaluator commanderLegalityEvaluator;
    private final DeckRevisionService deckRevisionService;
    private final PhysicalCardAllocationService allocationService;

    // Suppressed: cohesive deck-mutation collaborators (persistence, card lookups, ownership,
    // legality, and history recording); no natural subgrouping without an artificial wrapper, same
    // precedent as DeckController.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public DeckService(
            DeckRepository deckRepository,
            DeckCardRepository deckCardRepository,
            DeckAccessGuard deckAccessGuard,
            CardCatalogService cardCatalogService,
            CommanderLegalityEvaluator commanderLegalityEvaluator,
            DeckRevisionService deckRevisionService,
            PhysicalCardAllocationService allocationService) {
        this.deckRepository = deckRepository;
        this.deckCardRepository = deckCardRepository;
        this.deckAccessGuard = deckAccessGuard;
        this.cardCatalogService = cardCatalogService;
        this.commanderLegalityEvaluator = commanderLegalityEvaluator;
        this.deckRevisionService = deckRevisionService;
        this.allocationService = allocationService;
    }

    public List<DeckResponse> list() {
        return deckRepository.findByProfileIdOrderByNameAsc(profileId()).stream()
                .map(this::responseFor)
                .toList();
    }

    public DeckResponse create(DeckCreateRequest request) {
        Deck deck = new Deck(profileId(), request.name(), request.formatCode());
        deck.setDescription(request.description());
        deck.setCommanderCardId(request.commanderCardId());
        deck.setSecondaryCommanderCardId(request.secondaryCommanderCardId());
        deck.setUseOwnedCardsOnly(Boolean.TRUE.equals(request.useOwnedCardsOnly()));
        deck.setBudgetLimit(request.budgetLimit());
        deck.setDesiredPowerLevel(request.desiredPowerLevel());
        deck.setPlayStyle(request.playStyle());
        Deck saved = deckRepository.save(deck);
        deckRevisionService.record(saved.getId(), saved.getProfileId(), DeckChangeType.CREATED);
        return responseFor(saved);
    }

    public DeckResponse getById(long deckId) {
        return responseFor(owned(deckId));
    }

    public DeckLegalityResponse legality(long deckId) {
        Deck deck = owned(deckId);
        return commanderLegalityEvaluator.evaluate(deck, deckCardRepository.findByDeckId(deckId));
    }

    public DeckResponse update(long deckId, DeckUpdateRequest request) {
        Deck deck = deckAccessGuard.editableLocked(deckId);
        deckRevisionService.assertExpectedRevision(deckId, request.expectedRevision());
        MutableFields before = MutableFields.of(deck);
        applyCoreFields(deck, request);
        applyOptionFields(deck, request);
        Deck saved = deckRepository.save(deck);
        recordUpdate(saved, before);
        return responseFor(saved);
    }

    private void recordUpdate(Deck deck, MutableFields before) {
        MutableFields after = MutableFields.of(deck);
        if (before.equals(after)) {
            return;
        }
        boolean commanderChanged =
                !Objects.equals(before.commanderCardId(), after.commanderCardId())
                        || !Objects.equals(
                                before.secondaryCommanderCardId(),
                                after.secondaryCommanderCardId());
        DeckChangeType changeType =
                commanderChanged
                        ? DeckChangeType.COMMANDER_CHANGED
                        : DeckChangeType.METADATA_UPDATED;
        deckRevisionService.record(deck, deckAccessGuard.profileId(), changeType);
    }

    /** Snapshot of a deck's user-editable fields, used to detect no-op updates. */
    private record MutableFields(
            String name,
            String formatCode,
            @Nullable String description,
            @Nullable Long commanderCardId,
            @Nullable Long secondaryCommanderCardId,
            boolean useOwnedCardsOnly,
            @Nullable BigDecimal budgetLimit,
            @Nullable Integer desiredPowerLevel,
            @Nullable String playStyle) {

        static MutableFields of(Deck deck) {
            return new MutableFields(
                    deck.getName(),
                    deck.getFormatCode(),
                    deck.getDescription(),
                    deck.getCommanderCardId(),
                    deck.getSecondaryCommanderCardId(),
                    deck.isUseOwnedCardsOnly(),
                    deck.getBudgetLimit(),
                    deck.getDesiredPowerLevel(),
                    deck.getPlayStyle());
        }
    }

    private void applyCoreFields(Deck deck, DeckUpdateRequest request) {
        if (request.name() != null) {
            deck.setName(request.name());
        }
        if (request.formatCode() != null) {
            deck.setFormatCode(request.formatCode());
        }
        if (request.description() != null) {
            deck.setDescription(request.description());
        }
        if (request.commanderCardId() != null) {
            deck.setCommanderCardId(request.commanderCardId());
        }
        if (request.secondaryCommanderCardId() != null) {
            deck.setSecondaryCommanderCardId(request.secondaryCommanderCardId());
        }
    }

    private void applyOptionFields(Deck deck, DeckUpdateRequest request) {
        if (request.useOwnedCardsOnly() != null) {
            deck.setUseOwnedCardsOnly(request.useOwnedCardsOnly());
        }
        if (request.budgetLimit() != null) {
            deck.setBudgetLimit(request.budgetLimit());
        }
        if (request.desiredPowerLevel() != null) {
            deck.setDesiredPowerLevel(request.desiredPowerLevel());
        }
        if (request.playStyle() != null) {
            deck.setPlayStyle(request.playStyle());
        }
    }

    public void delete(long deckId) {
        allocationService.releaseDeck(deckId);
        deckRepository.delete(owned(deckId));
    }

    public DeckResponse archive(long deckId) {
        Deck deck = owned(deckId);
        boolean changed = deck.getStatus() != Deck.Status.ARCHIVED;
        deck.setStatus(Deck.Status.ARCHIVED);
        Deck saved = deckRepository.save(deck);
        if (changed) {
            deckRevisionService.record(
                    saved.getId(), saved.getProfileId(), DeckChangeType.METADATA_UPDATED);
        }
        return responseFor(saved);
    }

    public DeckResponse duplicate(long deckId) {
        Deck source = owned(deckId);
        Deck copy = new Deck(profileId(), source.getName() + " (Copy)", source.getFormatCode());
        copyDetails(source, copy);
        Deck saved = deckRepository.save(copy);
        copyCards(deckId, saved);
        deckRevisionService.record(saved.getId(), saved.getProfileId(), DeckChangeType.CREATED);
        return responseFor(saved);
    }

    private void copyDetails(Deck source, Deck copy) {
        copy.setDescription(source.getDescription());
        copy.setCommanderCardId(source.getCommanderCardId());
        copy.setSecondaryCommanderCardId(source.getSecondaryCommanderCardId());
        copy.setUseOwnedCardsOnly(source.isUseOwnedCardsOnly());
        copy.setBudgetLimit(source.getBudgetLimit());
        copy.setDesiredPowerLevel(source.getDesiredPowerLevel());
        copy.setPlayStyle(source.getPlayStyle());
    }

    private void copyCards(long sourceDeckId, Deck copy) {
        deckCardRepository.findByDeckId(sourceDeckId).stream()
                .map(card -> copyCard(card, copy.getId()))
                .forEach(deckCardRepository::save);
    }

    private DeckCard copyCard(DeckCard card, long deckId) {
        DeckCard copy =
                new DeckCard(
                        deckId,
                        card.getCardPrintingId(),
                        card.getQuantity(),
                        card.getDeckSection());
        copy.setOwnershipStatus(card.getOwnershipStatus());
        return copy;
    }

    private Long profileId() {
        return deckAccessGuard.profileId();
    }

    private Deck owned(long deckId) {
        return deckAccessGuard.owned(deckId);
    }

    private DeckResponse responseFor(Deck deck) {
        int cardCount =
                deckCardRepository.findByDeckId(deck.getId()).stream()
                        .mapToInt(DeckCard::getQuantity)
                        .sum();
        String commanderName =
                deck.getCommanderCardId() == null
                        ? null
                        : cardCatalogService.getNameById(deck.getCommanderCardId());
        return DeckResponse.from(
                deck,
                cardCount,
                commanderName,
                commanderSummary(deck.getCommanderCardId()),
                deckRevisionService.currentRevisionNumberUnchecked(deck.getId()));
    }

    private @Nullable CardSummaryResponse commanderSummary(@Nullable Long commanderCardId) {
        if (commanderCardId == null) {
            return null;
        }
        Long printingId = latestPrintingId(commanderCardId);
        return printingId == null ? null : summaryOrNull(printingId);
    }

    private @Nullable Long latestPrintingId(long cardId) {
        return cardCatalogService.getLatestPrintingIdByCardIds(List.of(cardId)).get(cardId);
    }

    private @Nullable CardSummaryResponse summaryOrNull(long printingId) {
        try {
            return cardCatalogService.getSummaryByPrintingId(printingId);
        } catch (CardNotFoundException exception) {
            return null;
        }
    }
}
