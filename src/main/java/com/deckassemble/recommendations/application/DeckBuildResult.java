package com.deckassemble.recommendations.application;

import com.deckassemble.decks.application.DeckLegalityResponse;
import com.deckassemble.decks.application.DeckResponse;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record DeckBuildResult(
        DeckResponse deck,
        int cardCount,
        int ownedCount,
        int wishlistCount,
        List<String> gaps,
        @Nullable BigDecimal score,
        DeckLegalityResponse legality,
        List<ScoredCandidate> scoredCandidates) {}
