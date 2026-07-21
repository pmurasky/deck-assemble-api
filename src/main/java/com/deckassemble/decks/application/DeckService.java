package com.deckassemble.decks.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeckService {

    private final DeckRepository deckRepository;
    private final DeckCardRepository deckCardRepository;
    private final CurrentUser currentUser;
    private final ProfileService profileService;
    private final CardCatalogService cardCatalogService;
    private final CommanderLegalityEvaluator commanderLegalityEvaluator;

    // Suppressed: six collaborators is what this orchestration service needs; Spring injects them.
    @SuppressWarnings("checkstyle:ParameterNumber")
    public DeckService(
            DeckRepository deckRepository,
            DeckCardRepository deckCardRepository,
            CurrentUser currentUser,
            ProfileService profileService,
            CardCatalogService cardCatalogService,
            CommanderLegalityEvaluator commanderLegalityEvaluator) {
        this.deckRepository = deckRepository;
        this.deckCardRepository = deckCardRepository;
        this.currentUser = currentUser;
        this.profileService = profileService;
        this.cardCatalogService = cardCatalogService;
        this.commanderLegalityEvaluator = commanderLegalityEvaluator;
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
        return responseFor(deckRepository.save(deck));
    }

    public DeckResponse getById(long deckId) {
        return responseFor(owned(deckId));
    }

    public DeckLegalityResponse legality(long deckId) {
        Deck deck = owned(deckId);
        return commanderLegalityEvaluator.evaluate(deck, deckCardRepository.findByDeckId(deckId));
    }

    public DeckResponse update(long deckId, DeckUpdateRequest request) {
        Deck deck = owned(deckId);
        applyCoreFields(deck, request);
        applyOptionFields(deck, request);
        return responseFor(deckRepository.save(deck));
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
        deckRepository.delete(owned(deckId));
    }

    public DeckResponse archive(long deckId) {
        Deck deck = owned(deckId);
        deck.setStatus(Deck.Status.ARCHIVED);
        return responseFor(deckRepository.save(deck));
    }

    public DeckResponse duplicate(long deckId) {
        Deck source = owned(deckId);
        Deck copy = new Deck(profileId(), source.getName() + " (Copy)", source.getFormatCode());
        copyDetails(source, copy);
        Deck saved = deckRepository.save(copy);
        copyCards(deckId, saved);
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
                .map(
                        card ->
                                new DeckCard(
                                        copy.getId(),
                                        card.getCardPrintingId(),
                                        card.getQuantity(),
                                        card.getDeckSection()))
                .forEach(deckCardRepository::save);
    }

    public List<DeckCardResponse> listCards(long deckId) {
        owned(deckId);
        return deckCardRepository.findByDeckId(deckId).stream().map(this::responseFor).toList();
    }

    public DeckCardResponse addCard(long deckId, DeckCardAddRequest request) {
        owned(deckId);
        DeckCard.Section section =
                request.deckSection() == null ? DeckCard.Section.MAIN_DECK : request.deckSection();
        int quantity = request.quantity() == null ? 1 : request.quantity();
        return responseFor(deckCardRepository.save(mergeOrNew(deckId, request, section, quantity)));
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
                .orElseGet(() -> new DeckCard(deckId, request.cardPrintingId(), quantity, section));
    }

    public DeckCardResponse updateCard(
            long deckId, long deckCardId, DeckCardUpdateRequest request) {
        owned(deckId);
        DeckCard card = ownedCard(deckId, deckCardId);
        if (request.quantity() != null) {
            card.setQuantity(request.quantity());
        }
        if (request.deckSection() != null) {
            card.setDeckSection(request.deckSection());
        }
        return responseFor(deckCardRepository.save(card));
    }

    public void removeCard(long deckId, long deckCardId) {
        owned(deckId);
        deckCardRepository.delete(ownedCard(deckId, deckCardId));
    }

    private Deck owned(long deckId) {
        return deckRepository
                .findByIdAndProfileId(deckId, profileId())
                .orElseThrow(DeckNotFoundException::new);
    }

    private DeckCard ownedCard(long deckId, long deckCardId) {
        return deckCardRepository
                .findByIdAndDeckId(deckCardId, deckId)
                .orElseThrow(DeckCardNotFoundException::new);
    }

    private Long profileId() {
        String subject =
                currentUser
                        .subject()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return profileService.getOrCreate(subject).getId();
    }

    private DeckCardResponse responseFor(DeckCard card) {
        return DeckCardResponse.from(
                card, cardCatalogService.getSummaryByPrintingId(card.getCardPrintingId()));
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
        return DeckResponse.from(deck, cardCount, commanderName);
    }
}
