package com.deckassemble.recommendations.application;

import com.deckassemble.decks.application.DeckAccessGuard;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

@Service
public class DeckBuilderService {

    private static final int DECK_SIZE = 100;
    private final DeckAccessGuard deckAccessGuard;
    private final CommanderResolver commanderResolver;
    private final DeckCandidateSelector deckCandidateSelector;
    private final BasicLandPadder basicLandPadder;
    private final DeckBuildRecorder deckBuildRecorder;

    public DeckBuilderService(
            DeckAccessGuard deckAccessGuard,
            CommanderResolver commanderResolver,
            DeckCandidateSelector deckCandidateSelector,
            BasicLandPadder basicLandPadder,
            DeckBuildRecorder deckBuildRecorder) {
        this.deckAccessGuard = deckAccessGuard;
        this.commanderResolver = commanderResolver;
        this.deckCandidateSelector = deckCandidateSelector;
        this.basicLandPadder = basicLandPadder;
        this.deckBuildRecorder = deckBuildRecorder;
    }

    public DeckBuildResult build(DeckBuildRequest request) {
        var profileId = deckAccessGuard.profileId();
        var commanders = commanderResolver.resolve(request);
        var identity = CommanderResolver.colorIdentity(commanders);
        var candidates = deckCandidateSelector.select(request, commanders, identity, profileId);
        var gaps = new ArrayList<String>();
        var targetSize = DECK_SIZE - commanders.size();
        var picked =
                DeckDraftPicker.pick(
                        candidates, targetSize, PlayStyleQuotas.forStyle(request.playStyle()));
        if (picked.isEmpty()) {
            gaps.add(
                    "No eligible cards found in your collection for this commander's color"
                            + " identity — 0 spells drafted; deck padded with basic lands");
        }
        var finalCards = basicLandPadder.pad(picked, identity, targetSize, gaps);
        return deckBuildRecorder.record(request, commanders, finalCards, gaps);
    }
}
