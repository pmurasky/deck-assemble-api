package com.deckassemble.decks.application.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Computes a canonical diff between two deck snapshots: which metadata fields changed, which cards
 * were added/removed/had their quantity change, and which category or tag names were added/removed.
 * Pure computation over already-loaded {@link DeckSnapshot}s; access control and revision lookup
 * belong to {@link DeckRevisionService}.
 */
@Service
public class DeckRevisionDiffService {

    private final DeckRevisionService deckRevisionService;

    public DeckRevisionDiffService(DeckRevisionService deckRevisionService) {
        this.deckRevisionService = deckRevisionService;
    }

    /**
     * Diffs two revisions of an owned deck, from {@code revisionNumber} to {@code
     * otherRevisionNumber}.
     */
    public Diff diff(long deckId, int revisionNumber, int otherRevisionNumber) {
        DeckSnapshot from = deckRevisionService.snapshotAt(deckId, revisionNumber);
        DeckSnapshot to = deckRevisionService.snapshotAt(deckId, otherRevisionNumber);
        return diff(from, to);
    }

    Diff diff(DeckSnapshot from, DeckSnapshot to) {
        return new Diff(
                metadataChanges(from, to),
                cardChanges(from.cards(), to.cards()),
                setDiff(from.categoryNames(), to.categoryNames()),
                setDiff(from.tagNames(), to.tagNames()));
    }

    private static List<FieldChange> metadataChanges(DeckSnapshot from, DeckSnapshot to) {
        List<FieldChange> changes = new ArrayList<>();
        addCoreFieldChanges(changes, from, to);
        addOptionFieldChanges(changes, from, to);
        return changes;
    }

    private static void addCoreFieldChanges(
            List<FieldChange> changes, DeckSnapshot from, DeckSnapshot to) {
        addIfChanged(changes, "name", from.name(), to.name());
        addIfChanged(changes, "formatCode", from.formatCode(), to.formatCode());
        addIfChanged(changes, "description", from.description(), to.description());
        addIfChanged(changes, "commanderCardId", from.commanderCardId(), to.commanderCardId());
        addIfChanged(
                changes,
                "secondaryCommanderCardId",
                from.secondaryCommanderCardId(),
                to.secondaryCommanderCardId());
        addIfChanged(changes, "folderId", from.folderId(), to.folderId());
    }

    private static void addOptionFieldChanges(
            List<FieldChange> changes, DeckSnapshot from, DeckSnapshot to) {
        addIfChanged(
                changes, "useOwnedCardsOnly", from.useOwnedCardsOnly(), to.useOwnedCardsOnly());
        addIfChanged(changes, "budgetLimit", from.budgetLimit(), to.budgetLimit());
        addIfChanged(
                changes, "desiredPowerLevel", from.desiredPowerLevel(), to.desiredPowerLevel());
        addIfChanged(changes, "playStyle", from.playStyle(), to.playStyle());
        addIfChanged(changes, "status", from.status(), to.status());
    }

    private static void addIfChanged(
            List<FieldChange> changes,
            String field,
            @Nullable Object before,
            @Nullable Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(new FieldChange(field, asString(before), asString(after)));
        }
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value == null ? null : value.toString();
    }

    private static CardDiff cardChanges(
            List<DeckSnapshot.CardEntry> from, List<DeckSnapshot.CardEntry> to) {
        Map<CardKey, Integer> fromQty = quantitiesByKey(from);
        Map<CardKey, Integer> toQty = quantitiesByKey(to);
        List<CardChange> added =
                onlyIn(toQty, fromQty).stream()
                        .map(key -> change(key, null, toQty.get(key)))
                        .toList();
        List<CardChange> removed =
                onlyIn(fromQty, toQty).stream()
                        .map(key -> change(key, fromQty.get(key), null))
                        .toList();
        List<CardChange> changed =
                changedKeys(fromQty, toQty).stream()
                        .map(key -> change(key, fromQty.get(key), toQty.get(key)))
                        .toList();
        return new CardDiff(added, removed, changed);
    }

    private static Map<CardKey, Integer> quantitiesByKey(List<DeckSnapshot.CardEntry> entries) {
        return entries.stream()
                .collect(
                        Collectors.toMap(
                                entry -> new CardKey(entry.cardPrintingId(), entry.deckSection()),
                                DeckSnapshot.CardEntry::quantity,
                                (first, second) -> first));
    }

    private static Set<CardKey> onlyIn(Map<CardKey, Integer> present, Map<CardKey, Integer> other) {
        return present.keySet().stream()
                .filter(key -> !other.containsKey(key))
                .collect(Collectors.toSet());
    }

    private static Set<CardKey> changedKeys(Map<CardKey, Integer> from, Map<CardKey, Integer> to) {
        return from.keySet().stream()
                .filter(key -> to.containsKey(key) && !to.get(key).equals(from.get(key)))
                .collect(Collectors.toSet());
    }

    private static CardChange change(
            CardKey key, @Nullable Integer quantityBefore, @Nullable Integer quantityAfter) {
        return new CardChange(
                key.cardPrintingId(), key.deckSection(), quantityBefore, quantityAfter);
    }

    private static NameSetDiff setDiff(List<String> from, List<String> to) {
        Set<String> fromSet = Set.copyOf(from);
        Set<String> toSet = Set.copyOf(to);
        List<String> added =
                toSet.stream().filter(name -> !fromSet.contains(name)).sorted().toList();
        List<String> removed =
                fromSet.stream().filter(name -> !toSet.contains(name)).sorted().toList();
        return new NameSetDiff(added, removed);
    }

    private record CardKey(Long cardPrintingId, String deckSection) {}

    /** Everything that differs between two deck snapshots. */
    public record Diff(
            List<FieldChange> metadataChanges,
            CardDiff cards,
            NameSetDiff categories,
            NameSetDiff tags) {}

    /** A single metadata field that changed, as before/after string values (nulls preserved). */
    public record FieldChange(String field, @Nullable String before, @Nullable String after) {}

    /** Cards added, removed, or with a changed quantity, keyed by printing + section. */
    public record CardDiff(
            List<CardChange> added, List<CardChange> removed, List<CardChange> changed) {}

    /**
     * One card's before/after quantity; a null side means the card wasn't present in that snapshot.
     */
    public record CardChange(
            Long cardPrintingId,
            String deckSection,
            @Nullable Integer quantityBefore,
            @Nullable Integer quantityAfter) {}

    /** Names present in one snapshot's set but not the other (categories or tags). */
    public record NameSetDiff(List<String> added, List<String> removed) {}
}
