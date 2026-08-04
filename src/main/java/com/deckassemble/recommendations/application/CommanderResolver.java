package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CommanderEligibility;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Resolves and validates commander cards for a build and derives their combined color identity. */
@Service
public class CommanderResolver {

    private final CardCatalogService cardCatalogService;

    public CommanderResolver(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }

    public List<Card> resolve(DeckBuildRequest request) {
        var commanders = new ArrayList<Card>();
        commanders.add(cardCatalogService.getCardWithFaces(request.commanderCardId()));
        if (request.secondaryCommanderCardId() != null) {
            commanders.add(cardCatalogService.getCardWithFaces(request.secondaryCommanderCardId()));
        }
        commanders.forEach(CommanderResolver::requireEligible);
        return commanders;
    }

    public static Set<String> colorIdentity(List<Card> commanders) {
        var identity = new HashSet<String>();
        for (var commander : commanders) {
            if (commander.getColorIdentity() != null) {
                for (var color : commander.getColorIdentity().split(",")) {
                    if (!color.isBlank()) {
                        identity.add(color.trim());
                    }
                }
            }
        }
        return identity;
    }

    private static void requireEligible(Card card) {
        if (!CommanderEligibility.isEligible(card)) {
            throw new IllegalArgumentException(
                    "Card is not eligible as commander: " + card.getName());
        }
    }
}
