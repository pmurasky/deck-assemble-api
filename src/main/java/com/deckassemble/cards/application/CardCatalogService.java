package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class CardCatalogService {

    private final CardRepository cardRepository;
    private final CardPrintingRepository cardPrintingRepository;

    public CardCatalogService(
            CardRepository cardRepository, CardPrintingRepository cardPrintingRepository) {
        this.cardRepository = cardRepository;
        this.cardPrintingRepository = cardPrintingRepository;
    }

    public Page<CardSummaryResponse> search(
            String query, String setCode, String colorIdentity, String type, Pageable pageable) {
        return cardRepository
                .findAll(specification(query, setCode, colorIdentity, type), pageable)
                .map(card -> CardSummaryResponse.from(card, latestPrinting(card.getId())));
    }

    // ponytail: one printing lookup per card (N+1 at page size); batch fetch if pages get slow
    private @Nullable CardPrinting latestPrinting(long cardId) {
        return cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(cardId).stream()
                .findFirst()
                .orElse(null);
    }

    private Specification<Card> specification(
            String query, String setCode, String colorIdentity, String type) {
        Specification<Card> result = activeSpec().and(nameSpec(query));
        if (colorIdentity != null) {
            result = result.and(colorIdentitySpec(colorIdentity));
        }
        if (setCode != null) {
            result = result.and(setCodeSpec(setCode));
        }
        if (type != null) {
            result = result.and(typeLineSpec(type));
        }
        return result;
    }

    private Specification<Card> activeSpec() {
        return (root, criteria, builder) -> builder.isTrue(root.get("active"));
    }

    private Specification<Card> nameSpec(String query) {
        return (root, criteria, builder) ->
                builder.like(builder.lower(root.get("name")), "%" + query.toLowerCase() + "%");
    }

    private Specification<Card> colorIdentitySpec(String colorIdentity) {
        return (root, criteria, builder) ->
                builder.like(root.get("colorIdentity"), "%" + colorIdentity + "%");
    }

    private Specification<Card> typeLineSpec(String type) {
        return (root, criteria, builder) ->
                builder.like(builder.lower(root.get("typeLine")), "%" + type.toLowerCase() + "%");
    }

    private Specification<Card> setCodeSpec(String setCode) {
        return (root, criteria, builder) -> {
            var subquery = criteria.subquery(Long.class);
            var printings = subquery.from(CardPrinting.class);
            return builder.exists(
                    subquery.select(printings.get("id"))
                            .where(
                                    builder.equal(printings.get("card").get("id"), root.get("id")),
                                    builder.equal(
                                            printings.get("magicSet").get("setCode"), setCode)));
        };
    }

    public CardDetailResponse getById(long cardId) {
        return cardRepository
                .findById(cardId)
                .filter(card -> card.getActive())
                .map(card -> CardDetailResponse.from(card, latestPrinting(card.getId())))
                .orElseThrow(CardNotFoundException::new);
    }

    public CardSummaryResponse getSummaryByPrintingId(long cardPrintingId) {
        return cardPrintingRepository
                .findById(cardPrintingId)
                .filter(printing -> printing.getActive() && printing.getCard().getActive())
                .map(printing -> CardSummaryResponse.from(printing.getCard(), printing))
                .orElseThrow(CardNotFoundException::new);
    }

    public @Nullable String getNameById(long cardId) {
        return cardRepository.findById(cardId).map(Card::getName).orElse(null);
    }

    public void validateFinishAvailability(
            long cardPrintingId, int regularQuantity, int foilQuantity) {
        CardPrinting printing =
                cardPrintingRepository
                        .findById(cardPrintingId)
                        .filter(p -> p.getActive() && p.getCard().getActive())
                        .orElseThrow(CardNotFoundException::new);
        if (regularQuantity > 0 && Boolean.FALSE.equals(printing.getNonfoilAvailable())) {
            throw new FinishUnavailableException("nonfoil");
        }
        if (foilQuantity > 0 && Boolean.FALSE.equals(printing.getFoilAvailable())) {
            throw new FinishUnavailableException("foil");
        }
    }

    public Page<CardSummaryResponse> getSetPrintings(String setCode, Pageable pageable) {
        return getSetPrintings(setCode, "", pageable);
    }

    public Page<CardSummaryResponse> getSetPrintings(
            String setCode, String query, Pageable pageable) {
        Page<CardPrinting> printings =
                query.isBlank()
                        ? cardPrintingRepository
                                .findByMagicSetSetCodeAndActiveTrueAndCardActiveTrue(
                                        setCode, pageable)
                        : cardPrintingRepository
                                .findByMagicSetSetCodeAndActiveTrueAndCardActiveTrueAndCardNameContainingIgnoreCase(
                                        setCode, query, pageable);
        return printings.map(printing -> CardSummaryResponse.from(printing.getCard(), printing));
    }

    public List<CardPrintingResponse> getPrintings(long cardId) {
        getById(cardId);
        return cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(cardId).stream()
                .map(CardPrintingResponse::from)
                .toList();
    }
}
