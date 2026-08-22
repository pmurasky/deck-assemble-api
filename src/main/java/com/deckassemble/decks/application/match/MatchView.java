package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.decks.application.match.MatchResponse.CardView;
import com.deckassemble.decks.application.match.MatchResponse.PermanentView;
import com.deckassemble.decks.application.match.MatchResponse.PlayerView;
import java.util.List;

/** Maps a match to the response for one player, enforcing hidden-information rules. */
public final class MatchView {

    private MatchView() {}

    /** Builds the response for the given player; the opponent's hand contents stay hidden. */
    public static MatchResponse forPlayer(Match match, PlayerId viewer) {
        PlayerState you = match.player(viewer);
        PlayerState opponent = match.opponentOf(viewer);
        return new MatchResponse(
                match.id(),
                match.turnNumber(),
                match.step().stepName(),
                match.activePlayer().playerId(),
                playerView(you, true),
                playerView(opponent, false),
                match.winner(),
                match.loser());
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
                player.landPlayedThisTurn());
    }

    private static List<CardView> cardViews(List<PracticeCard> cards) {
        return cards.stream().map(CardView::of).toList();
    }
}
