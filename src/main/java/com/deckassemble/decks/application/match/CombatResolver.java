package com.deckassemble.decks.application.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combat resolution: declared attackers, blocker validation, and immediate auto-assigned damage.
 * Explicit damage order, keywords, and state-based actions arrive in #47.
 */
final class CombatResolver {

    private final List<Permanent> pendingAttackers = new ArrayList<>();

    /** Each attacking printing must be on the attacker's battlefield; attackers tap. */
    void declareAttackers(PlayerState attacker, List<Long> printingIds) {
        for (long printingId : printingIds) {
            Permanent creature = findAttacker(attacker, printingId);
            creature.tap();
            pendingAttackers.add(creature);
        }
    }

    /**
     * Validates the defender's blockers (blocker printing -> attacker printing) and resolves damage
     * immediately: each attacker assigns lethal to its blockers in order (excess is lost without
     * trample), blockers deal their power back, and unblocked attackers hit the defender.
     */
    void declareBlockers(
            PlayerState attacker,
            PlayerState defender,
            Map<Long, Long> blockerToAttacker,
            Match match) {
        validateBlockers(defender, blockerToAttacker);
        Map<Permanent, Integer> damageMarked = resolveCombatDamage(defender, blockerToAttacker);
        buryDestroyedPermanents(attacker, defender, damageMarked);
        pendingAttackers.clear();
        markDefeatedPlayers(attacker, defender, match);
    }

    /** Drops any declared attackers that never resolved (e.g. the turn moved on). */
    void reset() {
        pendingAttackers.clear();
    }

    private Permanent findAttacker(PlayerState attacker, long printingId) {
        return attacker.battlefield().stream()
                .filter(permanent -> permanent.card().printingId() == printingId)
                .filter(permanent -> !pendingAttackers.contains(permanent))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("attacker is not on the battlefield"));
    }

    private void validateBlockers(PlayerState defender, Map<Long, Long> blockerToAttacker) {
        for (Map.Entry<Long, Long> assignment : blockerToAttacker.entrySet()) {
            findOnBattlefield(defender, assignment.getKey(), "blocker is not on the battlefield");
            boolean attacking =
                    pendingAttackers.stream()
                            .anyMatch(
                                    attacker ->
                                            attacker.card().printingId() == assignment.getValue());
            if (!attacking) {
                throw new IllegalArgumentException("block target is not attacking");
            }
        }
    }

    private Map<Permanent, Integer> resolveCombatDamage(
            PlayerState defender, Map<Long, Long> blockerToAttacker) {
        Map<Permanent, Integer> damageMarked = new ConcurrentHashMap<>();
        for (Permanent attacker : pendingAttackers) {
            List<Permanent> blockers = blockersFor(defender, attacker, blockerToAttacker);
            if (blockers.isEmpty()) {
                defender.takeCombatDamage(
                        attacker.power(), attacker.commander() ? attacker.controller() : null);
            } else {
                assignDamage(attacker, blockers, damageMarked);
            }
        }
        return damageMarked;
    }

    private List<Permanent> blockersFor(
            PlayerState defender, Permanent attacker, Map<Long, Long> blockerToAttacker) {
        return blockerToAttacker.entrySet().stream()
                .filter(assignment -> assignment.getValue() == attacker.card().printingId())
                .map(
                        assignment ->
                                findOnBattlefield(
                                        defender, assignment.getKey(), "blocker not found"))
                .toList();
    }

    private void assignDamage(
            Permanent attacker, List<Permanent> blockers, Map<Permanent, Integer> damageMarked) {
        int remaining = attacker.power();
        for (Permanent blocker : blockers) {
            damageMarked.merge(attacker, blocker.power(), Integer::sum);
            if (remaining <= 0) {
                continue;
            }
            int assigned = Math.min(blocker.toughness(), remaining);
            damageMarked.merge(blocker, assigned, Integer::sum);
            remaining -= assigned;
        }
    }

    private void buryDestroyedPermanents(
            PlayerState attacker, PlayerState defender, Map<Permanent, Integer> damageMarked) {
        for (PlayerState player : List.of(attacker, defender)) {
            List<Permanent> destroyed =
                    player.battlefield().stream()
                            .filter(
                                    permanent ->
                                            damageMarked.getOrDefault(permanent, 0)
                                                    >= permanent.toughness())
                            .toList();
            player.battlefield().removeAll(destroyed);
            destroyed.forEach(permanent -> player.graveyard().add(permanent.card()));
        }
    }

    private Permanent findOnBattlefield(PlayerState player, long printingId, String error) {
        return player.battlefield().stream()
                .filter(permanent -> permanent.card().printingId() == printingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(error));
    }

    private void markDefeatedPlayers(PlayerState attacker, PlayerState defender, Match match) {
        for (PlayerState player : List.of(attacker, defender)) {
            if (match.loser() == null && player.isDefeated()) {
                match.markLoser(player.playerId());
            }
        }
    }
}
