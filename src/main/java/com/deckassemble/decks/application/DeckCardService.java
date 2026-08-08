package com.deckassemble.decks.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardNotFoundException;
import com.deckassemble.cards.application.CardSummaryResponse;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages the cards within a deck: listing, adding, updating, and removing. */
@Service
@Transactional
public class DeckCardService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCardRepository deckCardRepository;
    private final CardCatalogService cardCatalogService;
    private final OwnershipChecker ownershipChecker;
    private final DeckRevisionService deckRevisionService;

    public DeckCardService(
            DeckAccessGuard deckAccessGuard,
            DeckCardRepository deckCardRepository,
            CardCatalogService cardCatalogService,
            OwnershipChecker ownershipChecker,
            DeckRevisionService deckRevisionService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCardRepository = deckCardRepository;
        this.cardCatalogService = cardCatalogService;
        this.ownershipChecker = ownershipChecker;
        this.deckRevisionService = deckRevisionService;
    }

    public List<DeckCardResponse> listCards(long deckId) {
        Deck deck = deckAccessGuard.owned(deckId);
        List<DeckCard> cards = deckCardRepository.findByDeckId(deckId);
        var responses = new java.util.ArrayList<>(cards.stream().map(this::responseFor).toList());
        addSynthesizedCommander(deck, cards, responses);
        return responses;
    }

    public DeckCardResponse addCard(long deckId, DeckCardAddRequest request) {
        deckAccessGuard.owned(deckId);
        DeckCard.Section section =
                request.deckSection() == null ? DeckCard.Section.MAIN_DECK : request.deckSection();
        int quantity = request.quantity() == null ? 1 : request.quantity();
        DeckCardResponse response =
                responseFor(
                        deckCardRepository.save(mergeOrNew(deckId, request, section, quantity)));
        deckRevisionService.record(deckId, deckAccessGuard.profileId(), DeckChangeType.CARD_ADDED);
        return response;
    }

    public DeckCardResponse updateCard(
            long deckId, long deckCardId, DeckCardUpdateRequest request) {
        deckAccessGuard.owned(deckId);
        DeckCard card = ownedCard(deckId, deckCardId);
        int oldQuantity = card.getQuantity();
        DeckCard.Section oldSection = card.getDeckSection();
        if (request.quantity() != null) {
            card.setQuantity(request.quantity());
        }
        if (request.deckSection() != null) {
            card.setDeckSection(request.deckSection());
        }
        DeckCardResponse response = responseFor(deckCardRepository.save(card));
        if (card.getQuantity() != oldQuantity || card.getDeckSection() != oldSection) {
            deckRevisionService.record(
                    deckId, deckAccessGuard.profileId(), DeckChangeType.CARD_UPDATED);
        }
        return response;
    }

    public void removeCard(long deckId, long deckCardId) {
        deckAccessGuard.owned(deckId);
        deckCardRepository.delete(ownedCard(deckId, deckCardId));
        deckRevisionService.record(
                deckId, deckAccessGuard.profileId(), DeckChangeType.CARD_REMOVED);
    }

    DeckCard ownedCard(long deckId, long deckCardId) {
        return deckCardRepository
                .findByIdAndDeckId(deckCardId, deckId)
                .orElseThrow(DeckCardNotFoundException::new);
    }

    DeckCardResponse responseFor(DeckCard card) {
        return DeckCardResponse.from(
                card, cardCatalogService.getSummaryByPrintingId(card.getCardPrintingId()));
    }

    // ponytail: commanders set only via commanderCardId have no DeckCard row; synthesize at read
    // time instead of backfilling rows. Upgrade path: persist COMMANDER rows on deck create/update.
    private void addSynthesizedCommander(
            Deck deck, List<DeckCard> cards, List<DeckCardResponse> responses) {
        Long commanderCardId = deck.getCommanderCardId();
        boolean hasCommanderRow =
                cards.stream()
                        .anyMatch(card -> card.getDeckSection() == DeckCard.Section.COMMANDER);
        if (commanderCardId == null || hasCommanderRow) {
            return;
        }
        Long printingId = latestPrintingId(commanderCardId);
        if (printingId == null) {
            return;
        }
        CardSummaryResponse summary = summaryOrNull(printingId);
        if (summary == null) {
            return;
        }
        responses.add(synthesizedCommander(printingId, summary));
    }

    private DeckCardResponse synthesizedCommander(long printingId, CardSummaryResponse summary) {
        String ownership =
                ownershipChecker.isOwned(deckAccessGuard.profileId(), printingId)
                        ? DeckCard.OwnershipStatus.OWNED.name()
                        : DeckCard.OwnershipStatus.WISHLIST.name();
        return new DeckCardResponse(
                null, printingId, 1, DeckCard.Section.COMMANDER.name(), ownership, summary);
    }

    private DeckCard mergeOrNew(
            long deckId, DeckCardAddRequest request, DeckCard.Section section, int quantity) {
        return deckCardRepository
                .findByDeckIdAndCardPrintingIdAndDeckSection(
                        deckId, request.cardPrintingId(), section)
                .map(
                        existing -> {
                            existing.setQuantity(existing.getQuantity() + quantity);
                            return existing;
                        })
                .orElseGet(() -> newCard(deckId, request, section, quantity));
    }

    private DeckCard newCard(
            long deckId, DeckCardAddRequest request, DeckCard.Section section, int quantity) {
        DeckCard card = new DeckCard(deckId, request.cardPrintingId(), quantity, section);
        card.setOwnershipStatus(
                ownershipChecker.isOwned(deckAccessGuard.profileId(), request.cardPrintingId())
                        ? DeckCard.OwnershipStatus.OWNED
                        : DeckCard.OwnershipStatus.WISHLIST);
        return card;
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
