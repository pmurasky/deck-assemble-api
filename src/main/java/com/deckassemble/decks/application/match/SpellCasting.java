package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.decks.application.simulation.DeckLibraryResolver;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/** Pushes spells onto the stack with priority-based timing and cast-time target validation. */
final class SpellCasting {

    private SpellCasting() {}

    static void castSpell(
            Match match,
            StackResolver stack,
            PlayerId casterId,
            long printingId,
            StackObject.@Nullable Target target) {
        if (match.loser() != null) {
            throw new IllegalArgumentException("match is over");
        }
        if (!stack.priorityHolder().equals(casterId)) {
            throw new IllegalArgumentException("player does not have priority");
        }
        PlayerState caster = match.player(casterId);
        PracticeCard card = removeCastableCard(caster, printingId);
        validateCastTiming(match, stack, caster, card);
        if (target != null && target.missingFrom(match)) {
            throw new IllegalArgumentException("target does not exist");
        }
        stack.push(new StackObject(casterId, card, target));
        stack.givePriorityTo(match.opponentOf(casterId).playerId());
    }

    private static PracticeCard removeCastableCard(PlayerState caster, long printingId) {
        PracticeCard commander = caster.commander();
        if (commander.printingId() == printingId && caster.commanderInCommandZone()) {
            caster.incrementCommanderTax();
            caster.setCommanderInCommandZone(false);
            return commander;
        }
        PracticeCard card = caster.requireInHand(printingId);
        if (DeckLibraryResolver.isLand(card.card())) {
            throw new IllegalArgumentException("lands are played, not cast");
        }
        caster.hand().remove(card);
        return card;
    }

    private static void validateCastTiming(
            Match match, StackResolver stack, PlayerState caster, PracticeCard card) {
        String typeLine = card.card().getTypeLine();
        if (typeLine != null && typeLine.toLowerCase(Locale.ROOT).contains("instant")) {
            return;
        }
        if (!match.activePlayer().equals(caster)
                || !isMainStep(match)
                || !stack.stack().isEmpty()) {
            throw new IllegalArgumentException(
                    card.card().getName() + " can only be cast at sorcery speed");
        }
    }

    private static boolean isMainStep(Match match) {
        return match.step() instanceof TurnStep.FirstMain
                || match.step() instanceof TurnStep.SecondMain;
    }
}
