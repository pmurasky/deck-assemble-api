package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Resolves supported external card references to Scryfall identifiers. */
@Service
public class CardReferenceResolver {

    private final CardRepository cardRepository;
    private final CardPrintingRepository printingRepository;

    public CardReferenceResolver(
            CardRepository cardRepository, CardPrintingRepository printingRepository) {
        this.cardRepository = Objects.requireNonNull(cardRepository);
        this.printingRepository = Objects.requireNonNull(printingRepository);
    }

    /** Resolves an exact Scryfall identifier or complete printing reference. */
    public CardReferenceResolution resolve(CardReference reference) {
        if (reference.scryfallId() != null) {
            var printing =
                    printingRepository.findByScryfallCardId(reference.scryfallId().toString());
            if (printing.isPresent()) {
                return resolution(List.of(printing.orElseThrow()), true);
            }
        }
        return resolvePrintingReference(reference);
    }

    private CardReferenceResolution resolvePrintingReference(CardReference reference) {
        if (reference.name() == null) {
            return new CardReferenceResolution.Unmatched();
        }
        if (reference.setCode() != null && reference.collectorNumber() != null) {
            var matches =
                    printingRepository
                            .findByCardNameIgnoreCaseAndMagicSetSetCodeIgnoreCaseAndCollectorNumberIgnoreCase(
                                    reference.name(),
                                    reference.setCode(),
                                    reference.collectorNumber());
            return resolution(matches, true);
        }
        if (reference.setCode() == null && reference.collectorNumber() == null) {
            return resolution(findByName(reference.name()), false);
        }
        return new CardReferenceResolution.Unmatched();
    }

    private List<CardPrinting> findByName(String name) {
        return cardRepository.findByNameIgnoreCase(name).stream()
                .flatMap(
                        card ->
                                printingRepository
                                        .findByCardIdOrderByReleasedAtDesc(card.getId())
                                        .stream())
                .toList();
    }

    private static CardReferenceResolution resolution(
            List<CardPrinting> matches, boolean exactReference) {
        if (matches.size() > 1) {
            return new CardReferenceResolution.Ambiguous(
                    matches.stream()
                            .map(CardPrinting::getScryfallCardId)
                            .map(UUID::fromString)
                            .toList());
        }
        if (matches.isEmpty() || !exactReference) {
            return new CardReferenceResolution.Unmatched();
        }
        var printing = matches.getFirst();
        return new CardReferenceResolution.Matched(
                UUID.fromString(printing.getCard().getScryfallOracleId()),
                UUID.fromString(printing.getScryfallCardId()));
    }
}
