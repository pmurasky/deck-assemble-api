package com.deckassemble.decks.application;

import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.decks.application.DeckLegalityResponse.Violation;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Legality evaluator for the Pauper constructed format: Standard-style constructed rules plus the
 * commons-only restriction.
 *
 * @since 1.0
 */
@Component
class PauperLegalityEvaluator extends ConstructedLegalityEvaluator {

    PauperLegalityEvaluator(CardPrintingRepository cardPrintingRepository) {
        super(cardPrintingRepository);
    }

    @Override
    public String formatCode() {
        return "PAUPER";
    }

    @Override
    protected void validatePrinting(CardPrinting printing, List<Violation> violations) {
        if (!"common".equalsIgnoreCase(printing.getRarity())) {
            violations.add(
                    new Violation(
                            "PAUPER_RARITY_VIOLATION",
                            printing.getCard().getName()
                                    + " is "
                                    + printing.getRarity()
                                    + "; Pauper allows commons only"));
        }
    }
}
