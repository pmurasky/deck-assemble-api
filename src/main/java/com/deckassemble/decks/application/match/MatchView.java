package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.decks.application.match.MatchResponse.CardView;
import com.deckassemble.decks.application.match.MatchResponse.PermanentView;
import com.deckassemble.decks.application.match.MatchResponse.PlayerView;
import com.deckassemble.decks.application.match.MatchResponse.StackObjectView;
import java.util.List;
import java.util.UUID;

/** Maps a match to the response for one player, enforcing hidden-information rules. */
public final class MatchView {

    private MatchView() {}

    /** Builds the response for the given player; the opponent's hand contents stay hidden. */
    public static MatchResponse forPlayer(Match match, PlayerId viewer) {
        PlayerState you = match.player(viewer);
        PlayerState opponent = match.opponentOf(viewer);
        PlayerId loser = match.loser();
        return new MatchResponse(
                match.id(),
                match.turnNumber(),
                match.step().stepName(),
                match.activePlayer().playerId(),
                playerView(you, true),
                playerView(opponent, false),
                loser == null ? null : match.opponentOf(loser).playerId(),
                loser,
                stackViews(match),
                match.stackResolver().priorityHolder());
    }

    private static PlayerView playerView(PlayerState player, boolean showHand) {
        return new PlayerView(
                player.playerId(),
                player.life(),
                player.hand().size(),
                player.library().size(),
                showHand ? cardViews(player.hand()) : null,
                player.battlefield().stream().map(PermanentView::of).toList(),
                cardViews(player.graveyard()),
                cardViews(player.exile()),
                CardView.of(player.commander()),
                player.commanderTax(),
                player.commanderDamageReceived(),
                player.landPlayedThisTurn(),
                player.autoPassEnabled());
    }

    private static List<StackObjectView> stackViews(Match match) {
        return match.stackResolver().stack().stream().map(MatchView::stackView).toList();
    }

    private static StackObjectView stackView(StackObject object) {
        Long targetPermanentId = null;
        UUID targetPlayerId = null;
        if (object.target() instanceof StackObject.Target.PermanentTarget permanent) {
            targetPermanentId = permanent.printingId();
        } else if (object.target() instanceof StackObject.Target.PlayerTarget player) {
            targetPlayerId = player.playerId().value();
        }
        return new StackObjectView(
                CardView.of(object.card()), object.controller(), targetPermanentId, targetPlayerId);
    }

    private static List<CardView> cardViews(List<PracticeCard> cards) {
        return cards.stream().map(CardView::of).toList();
    }
}
