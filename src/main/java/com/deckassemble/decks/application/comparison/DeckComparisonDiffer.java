package com.deckassemble.decks.application.comparison;

import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.comparison.DeckComparisonService.CardChange;
import com.deckassemble.decks.application.comparison.DeckComparisonService.QuantityChange;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Diffs two deck card lists by card identity (oracle id), not exact printing. */
// Justified: method-local map, never shared across threads.
@SuppressWarnings("PMD.UseConcurrentHashMap")
final class DeckComparisonDiffer {

    private DeckComparisonDiffer() {}

    static CompositionDiff diff(
            List<DeckCardResponse> baseCards, List<DeckCardResponse> otherCards) {
        Map<String, Identity> base = byIdentity(baseCards);
        Map<String, Identity> other = byIdentity(otherCards);
        return new CompositionDiff(added(base, other), removed(base, other), changed(base, other));
    }

    // ponytail: diff covers the playable deck (commander + main), matching analysis scope.
    // Add sideboard/companion/maybe to the comparison if users ask for them.
    private static Map<String, Identity> byIdentity(List<DeckCardResponse> cards) {
        Map<String, Identity> byIdentity = new HashMap<>();
        cards.stream()
                .filter(DeckComparisonDiffer::playable)
                .forEach(
                        card ->
                                byIdentity.merge(
                                        identityKey(card),
                                        new Identity(name(card), card.quantity()),
                                        Identity::plus));
        return byIdentity;
    }

    private static boolean playable(DeckCardResponse card) {
        return "COMMANDER".equals(card.deckSection()) || "MAIN_DECK".equals(card.deckSection());
    }

    // ponytail: cards with an unresolved catalog summary fall back to printing-id identity;
    // they can never match a resolved oracle identity. Upgrade path: backfill stale printings.
    private static String identityKey(DeckCardResponse card) {
        return card.card() == null ? "printing:" + card.cardPrintingId() : card.card().oracleId();
    }

    private static String name(DeckCardResponse card) {
        return card.card() == null
                ? "Unknown printing " + card.cardPrintingId()
                : card.card().name();
    }

    private static List<CardChange> added(Map<String, Identity> base, Map<String, Identity> other) {
        return other.entrySet().stream()
                .filter(entry -> !base.containsKey(entry.getKey()))
                .map(entry -> change(entry.getValue()))
                .sorted(Comparator.comparing(CardChange::name))
                .toList();
    }

    private static List<CardChange> removed(
            Map<String, Identity> base, Map<String, Identity> other) {
        return base.entrySet().stream()
                .filter(entry -> !other.containsKey(entry.getKey()))
                .map(entry -> change(entry.getValue()))
                .sorted(Comparator.comparing(CardChange::name))
                .toList();
    }

    private static List<QuantityChange> changed(
            Map<String, Identity> base, Map<String, Identity> other) {
        return base.entrySet().stream()
                .map(entry -> changeIfDifferent(entry.getValue(), other.get(entry.getKey())))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(QuantityChange::name))
                .toList();
    }

    private static Optional<QuantityChange> changeIfDifferent(
            Identity base, @Nullable Identity other) {
        return other == null || other.quantity() == base.quantity()
                ? Optional.empty()
                : Optional.of(new QuantityChange(base.name(), base.quantity(), other.quantity()));
    }

    private static CardChange change(Identity identity) {
        return new CardChange(identity.name(), identity.quantity());
    }

    record CompositionDiff(
            List<CardChange> added,
            List<CardChange> removed,
            List<QuantityChange> quantityChanged) {}

    private record Identity(String name, int quantity) {
        Identity plus(Identity other) {
            return new Identity(name, quantity + other.quantity);
        }
    }
}
