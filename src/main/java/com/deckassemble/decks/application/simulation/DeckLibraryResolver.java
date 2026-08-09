package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Resolves a deck snapshot's shuffleable library: main-deck cards only, commander(s) excluded by
 * resolved card identity (not just deck section, so a commander that's also miscategorized as a
 * MAIN_DECK row is still excluded), plus the land-detection rule {@link DeckSampleHandService}
 * needs to honor London mulligan land ranges. Pure computation over an already-loaded snapshot and
 * already-resolved card facts — no DB access of its own.
 */
final class DeckLibraryResolver {

    private static final String MAIN_DECK_SECTION = "MAIN_DECK";

    private DeckLibraryResolver() {}

    static List<DeckSnapshot.CardEntry> mainDeckEntries(DeckSnapshot snapshot) {
        return snapshot.cards().stream()
                .filter(entry -> MAIN_DECK_SECTION.equals(entry.deckSection()))
                .toList();
    }

    static List<Long> printingIdsOf(List<DeckSnapshot.CardEntry> entries) {
        return entries.stream().map(DeckSnapshot.CardEntry::cardPrintingId).distinct().toList();
    }

    static List<Long> expandLibrary(
            List<DeckSnapshot.CardEntry> entries,
            Map<Long, Card> cardsByPrinting,
            DeckSnapshot snapshot) {
        Set<Long> commanderCardIds = commanderCardIds(snapshot);
        List<Long> library = new ArrayList<>();
        for (DeckSnapshot.CardEntry entry : entries) {
            Card card = cardsByPrinting.get(entry.cardPrintingId());
            if (card != null && commanderCardIds.contains(card.getId())) {
                continue;
            }
            for (int i = 0; i < entry.quantity(); i++) {
                library.add(entry.cardPrintingId());
            }
        }
        return library;
    }

    private static Set<Long> commanderCardIds(DeckSnapshot snapshot) {
        Set<Long> ids = new HashSet<>();
        if (snapshot.commanderCardId() != null) {
            ids.add(snapshot.commanderCardId());
        }
        if (snapshot.secondaryCommanderCardId() != null) {
            ids.add(snapshot.secondaryCommanderCardId());
        }
        return ids;
    }

    // Reuses the same rule decks.application.analysis.AnalysisEntry#isLand already applies for
    // mana-curve/land-count analytics: lowercased type line (front face + all other faces)
    // contains "land". Reimplemented here (rather than depending on that package-private type)
    // against the Card facts this service already resolves for commander-identity exclusion.
    static boolean isLand(@Nullable Card card) {
        if (card == null) {
            return false;
        }
        StringBuilder typeLines = new StringBuilder();
        appendLowercased(typeLines, card.getTypeLine());
        card.getFaces().forEach(face -> appendLowercased(typeLines, face.getTypeLine()));
        return typeLines.toString().contains("land");
    }

    private static void appendLowercased(StringBuilder target, @Nullable String value) {
        if (value != null) {
            target.append(value.toLowerCase(Locale.ROOT)).append('\n');
        }
    }
}
