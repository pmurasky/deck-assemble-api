package com.deckassemble.decks.application.comparison;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.analysis.DeckAnalysisResponse;
import com.deckassemble.decks.application.analysis.DeckAnalysisService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Compares two owned decks: card composition diff plus analysis metric deltas. */
@Service
public class DeckComparisonService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCardService deckCardService;
    private final DeckAnalysisService deckAnalysisService;

    public DeckComparisonService(
            DeckAccessGuard deckAccessGuard,
            DeckCardService deckCardService,
            DeckAnalysisService deckAnalysisService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCardService = deckCardService;
        this.deckAnalysisService = deckAnalysisService;
    }

    public Comparison compare(long deckId, long otherDeckId) {
        deckAccessGuard.owned(deckId);
        deckAccessGuard.owned(otherDeckId);
        DeckComparisonDiffer.CompositionDiff diff =
                DeckComparisonDiffer.diff(
                        deckCardService.listCards(deckId), deckCardService.listCards(otherDeckId));
        DeckAnalysisResponse base = deckAnalysisService.analyze(deckId);
        DeckAnalysisResponse other = deckAnalysisService.analyze(otherDeckId);
        return Comparison.of(diff, base, other);
    }

    /** Full comparison: composition changes plus per-metric deltas (other minus base). */
    public record Comparison(
            List<CardChange> added,
            List<CardChange> removed,
            List<QuantityChange> quantityChanged,
            Map<String, Integer> ownershipDelta,
            Map<String, BigDecimal> valueDeltaByCurrency,
            Map<String, BigDecimal> missingCostDeltaByCurrency,
            Map<String, Integer> curveDelta,
            Map<String, Integer> categoryDelta,
            LegalityDelta legality,
            List<String> gameChangersAdded,
            List<String> gameChangersRemoved,
            ComboDelta combos) {

        static Comparison of(
                DeckComparisonDiffer.CompositionDiff diff,
                DeckAnalysisResponse base,
                DeckAnalysisResponse other) {
            return new Comparison(
                    diff.added(),
                    diff.removed(),
                    diff.quantityChanged(),
                    DeckComparisonCalculator.intDelta(
                            base.ownershipBreakdown(), other.ownershipBreakdown()),
                    DeckComparisonCalculator.decimalDelta(
                            base.valueByCurrency(), other.valueByCurrency()),
                    DeckComparisonCalculator.decimalDelta(
                            base.missingCostByCurrency(), other.missingCostByCurrency()),
                    DeckComparisonCalculator.intDelta(base.manaCurve(), other.manaCurve()),
                    DeckComparisonCalculator.intDelta(
                            base.functionalCategories(), other.functionalCategories()),
                    DeckComparisonCalculator.legalityDelta(base.legality(), other.legality()),
                    DeckComparisonCalculator.addedItems(base.gameChangers(), other.gameChangers()),
                    DeckComparisonCalculator.removedItems(
                            base.gameChangers(), other.gameChangers()),
                    DeckComparisonCalculator.comboDelta(base.combos(), other.combos()));
        }
    }

    public record CardChange(String name, int quantity) {}

    public record QuantityChange(String name, int fromQuantity, int toQuantity) {}

    public record LegalityDelta(
            boolean baseLegal,
            boolean otherLegal,
            List<String> addedViolations,
            List<String> removedViolations) {}

    public record ComboDelta(
            int baseCount,
            int otherCount,
            List<String> addedComboIds,
            List<String> removedComboIds) {}
}
