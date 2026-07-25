package com.deckassemble.recommendations.application;

import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DeckDraftPicker {

    static final Map<Category, Integer> QUOTAS = quotas();

    private DeckDraftPicker() {}

    public static List<DeckCandidate> pick(List<DeckCandidate> sortedCandidates, int slots) {
        var picked = new ArrayList<DeckCandidate>();
        var pickedOracles = new HashSet<String>();
        for (var entry : QUOTAS.entrySet()) {
            var quota = Math.min(entry.getValue(), slots - picked.size());
            pickCategory(sortedCandidates, entry.getKey(), quota, picked, pickedOracles);
        }
        pickCategory(sortedCandidates, Category.SYNERGY, slots - picked.size(), picked, pickedOracles);
        pickAny(sortedCandidates, slots - picked.size(), picked, pickedOracles);
        return picked;
    }

    private static void pickCategory(
            List<DeckCandidate> sorted,
            Category category,
            int quota,
            List<DeckCandidate> picked,
            Set<String> pickedOracles) {
        var remaining = quota;
        for (var candidate : sorted) {
            if (remaining == 0) {
                return;
            }
            if (candidate.category() == category
                    && pickedOracles.add(candidate.card().getScryfallOracleId())) {
                picked.add(candidate);
                remaining--;
            }
        }
    }

    private static void pickAny(
            List<DeckCandidate> sorted, int slots, List<DeckCandidate> picked, Set<String> oracles) {
        var remaining = slots;
        for (var candidate : sorted) {
            if (remaining == 0) {
                return;
            }
            if (oracles.add(candidate.card().getScryfallOracleId())) {
                picked.add(candidate);
                remaining--;
            }
        }
    }

    private static Map<Category, Integer> quotas() {
        Map<Category, Integer> quotas = new LinkedHashMap<>();
        quotas.put(Category.LAND, 36);
        quotas.put(Category.RAMP, 10);
        quotas.put(Category.DRAW, 10);
        quotas.put(Category.REMOVAL, 8);
        quotas.put(Category.WIPE, 3);
        return Collections.unmodifiableMap(quotas);
    }
}
