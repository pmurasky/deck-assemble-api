package com.deckassemble.decks.application;

import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.decks.application.DeckLegalityResponse.Violation;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Legality evaluator for the Standard constructed format.
 *
 * @since 1.0
 */
@Component
class StandardLegalityEvaluator extends ConstructedLegalityEvaluator {

    StandardLegalityEvaluator(CardPrintingRepository cardPrintingRepository) {
        super(cardPrintingRepository);
    }

    @Override
    public String formatCode() {
        return "STANDARD";
    }

    @Override
    protected void validatePrinting(CardPrinting printing, List<Violation> violations) {
        // No additional per-printing rules for Standard.
    }
}
