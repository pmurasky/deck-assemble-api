package com.deckassemble.recommendations.application;

import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

// Diminishing-returns greedy picker: a card's value drops as its role fills (need =
// 1 - filled/target), cliffs negative once the role target is met, and is further
// penalized when its mana-value bucket is over the soft cap.
public final class DeckDraftPicker {

    static final Map<Category, Integer> QUOTAS = quotas();

    private static final int LAND_QUOTA = 36;
    private static final int RAMP_QUOTA = 10;
    private static final int DRAW_QUOTA = 10;
    private static final int REMOVAL_QUOTA = 8;
    private static final int WIPE_QUOTA = 3;
    private static final int PROTECTION_QUOTA = 5;
    private static final int FINISHER_QUOTA = 4;
    private static final double ROLE_WEIGHT = 1.0;
    private static final double ROLE_OVERFLOW_NEED = -1.0;
    private static final double CURVE_WEIGHT = 0.5;
    private static final int CURVE_BUCKET_COUNT = 6;
    private static final int MV_ONE = 1;
    // ponytail: soft caps are guide midpoints for MV 0-1/2/3/4/5/6+ buckets; calibrate
    // against P5 deck-score outcomes before making configurable.
    private static final int[] CURVE_SOFT_CAPS = {8, 14, 14, 10, 7, 5};

    private DeckDraftPicker() {}

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // EnumMap is single-threaded local state; no concurrency needed
    public static List<DeckCandidate> pick(List<DeckCandidate> sortedCandidates, int slots) {
        return pick(sortedCandidates, slots, QUOTAS);
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // EnumMap is single-threaded local state; no concurrency needed
    public static List<DeckCandidate> pick(
            List<DeckCandidate> sortedCandidates, int slots, Map<Category, Integer> quotas) {
        var picked = new ArrayList<DeckCandidate>(slots);
        var pickedOracles = new HashSet<String>();
        Map<Category, Integer> filled = new EnumMap<>(Category.class);
        var curve = new int[CURVE_BUCKET_COUNT];
        var remaining = new ArrayList<>(sortedCandidates);
        while (picked.size() < slots && !remaining.isEmpty()) {
            var best = bestCandidate(remaining, pickedOracles, filled, curve, quotas);
            if (best == null) {
                break;
            }
            picked.add(best);
            pickedOracles.add(best.card().getScryfallOracleId());
            filled.merge(bestRole(best, filled, quotas), 1, Integer::sum);
            recordCurve(best, curve);
            remaining.remove(best);
        }
        return picked;
    }

    @Nullable private static DeckCandidate bestCandidate(
            List<DeckCandidate> remaining,
            Set<String> pickedOracles,
            Map<Category, Integer> filled,
            int[] curve,
            Map<Category, Integer> quotas) {
        DeckCandidate best = null;
        var bestScore = Double.NEGATIVE_INFINITY;
        for (var candidate : remaining) {
            var effective = effectiveScore(candidate, pickedOracles, filled, curve, quotas);
            if (effective != null && (best == null || effective > bestScore)) {
                best = candidate;
                bestScore = effective;
            }
        }
        return best;
    }

    @Nullable private static Double effectiveScore(
            DeckCandidate candidate,
            Set<String> pickedOracles,
            Map<Category, Integer> filled,
            int[] curve,
            Map<Category, Integer> quotas) {
        if (pickedOracles.contains(candidate.card().getScryfallOracleId())) {
            return null;
        }
        return candidate.scoreValue()
                + ROLE_WEIGHT * maxRoleNeed(candidate, filled, quotas)
                - CURVE_WEIGHT * curveExcess(candidate, curve);
    }

    private static double maxRoleNeed(
            DeckCandidate candidate, Map<Category, Integer> filled, Map<Category, Integer> quotas) {
        var max = 0.0;
        var any = false;
        for (var role : candidate.roles()) {
            if (quotas.containsKey(role)) {
                var need = roleNeed(role, filled, quotas);
                max = !any || need > max ? need : max;
                any = true;
            }
        }
        return max;
    }

    private static Category bestRole(
            DeckCandidate candidate, Map<Category, Integer> filled, Map<Category, Integer> quotas) {
        var best = Category.SYNERGY;
        var max = Double.NEGATIVE_INFINITY;
        for (var role : candidate.roles()) {
            if (quotas.containsKey(role)) {
                var need = roleNeed(role, filled, quotas);
                if (need > max) {
                    max = need;
                    best = role;
                }
            }
        }
        return best;
    }

    private static double roleNeed(
            Category role, Map<Category, Integer> filled, Map<Category, Integer> quotas) {
        var target = quotas.get(role);
        if (target == null) {
            return 0.0;
        }
        var count = filled.getOrDefault(role, 0);
        return count < target ? 1.0 - (double) count / target : ROLE_OVERFLOW_NEED;
    }

    private static double curveExcess(DeckCandidate candidate, int[] curve) {
        var manaValue = candidate.card().getManaValue();
        if (!countsTowardCurve(candidate, manaValue)) {
            return 0.0;
        }
        var bucket = bucketOf(manaValue.intValue());
        var excess = curve[bucket] - CURVE_SOFT_CAPS[bucket];
        return Math.max(0, (double) excess / CURVE_SOFT_CAPS[bucket]);
    }

    private static void recordCurve(DeckCandidate candidate, int[] curve) {
        var manaValue = candidate.card().getManaValue();
        if (countsTowardCurve(candidate, manaValue)) {
            curve[bucketOf(manaValue.intValue())]++;
        }
    }

    private static boolean countsTowardCurve(DeckCandidate candidate, @Nullable BigDecimal mv) {
        return mv != null && !candidate.roles().contains(Category.LAND);
    }

    private static int bucketOf(int manaValue) {
        if (manaValue <= MV_ONE) {
            return 0;
        }
        return Math.min(manaValue - 1, CURVE_BUCKET_COUNT - 1);
    }

    // Justified: method-local LinkedHashMap (insertion order required), never shared across
    // threads.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<Category, Integer> quotas() {
        Map<Category, Integer> quotas = new LinkedHashMap<>();
        quotas.put(Category.LAND, LAND_QUOTA);
        quotas.put(Category.RAMP, RAMP_QUOTA);
        quotas.put(Category.DRAW, DRAW_QUOTA);
        quotas.put(Category.REMOVAL, REMOVAL_QUOTA);
        quotas.put(Category.WIPE, WIPE_QUOTA);
        quotas.put(Category.PROTECTION, PROTECTION_QUOTA);
        quotas.put(Category.FINISHER, FINISHER_QUOTA);
        return Collections.unmodifiableMap(quotas);
    }
}
