package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The stack and priority: pass order, both-pass resolution, and step advancement. */
final class StackResolver {

    private final List<StackObject> stack = new ArrayList<>();
    private PlayerId priorityHolder;
    private int passesInSuccession;

    StackResolver(PlayerId activePlayer) {
        this.priorityHolder = activePlayer;
    }

    List<StackObject> stack() {
        return List.copyOf(stack);
    }

    PlayerId priorityHolder() {
        return priorityHolder;
    }

    void push(StackObject object) {
        stack.add(object);
        passesInSuccession = 0;
    }

    void passPriorityForHolder(Match match) {
        passPriority(match, priorityHolder);
    }

    void passPriority(Match match, PlayerId player) {
        if (match.loser() != null) {
            throw new IllegalArgumentException("match is over");
        }
        if (!priorityHolder.equals(player)) {
            throw new IllegalArgumentException("player does not have priority");
        }
        passesInSuccession++;
        if (passesInSuccession < 2) {
            priorityHolder = match.opponentOf(player).playerId();
            return;
        }
        passesInSuccession = 0;
        if (hasPendingMandatoryAction()) {
            return;
        }
        resolveOrAdvance(match);
        priorityHolder = match.activePlayer().playerId();
    }

    /** #47/#48 plug real mandatory-action checks in here; nothing is mandatory yet. */
    private boolean hasPendingMandatoryAction() {
        return false;
    }

    private void resolveOrAdvance(Match match) {
        if (stack.isEmpty()) {
            match.advanceStepNow();
            return;
        }
        resolveTop(match);
    }

    private void resolveTop(Match match) {
        StackObject top = stack.removeLast();
        PlayerState controller = match.player(top.controller());
        if (top.target() != null && top.target().missingFrom(match)) {
            controller.graveyard().add(top.card());
            return;
        }
        if (isCreature(top.card())) {
            boolean commander = top.card().printingId() == controller.commander().printingId();
            controller.battlefield().add(new Permanent(top.card(), top.controller(), commander));
            return;
        }
        // Non-creature spells resolve as a no-op placeholder until #49 wires effects.
        controller.graveyard().add(top.card());
    }

    private boolean isCreature(PracticeCard card) {
        String typeLine = card.card().getTypeLine();
        return typeLine != null && typeLine.toLowerCase(Locale.ROOT).contains("creature");
    }
}
