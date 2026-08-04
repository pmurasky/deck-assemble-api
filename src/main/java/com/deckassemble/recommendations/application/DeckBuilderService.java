package com.deckassemble.recommendations.application;

import com.deckassemble.decks.application.DeckAccessGuard;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

// Justified: orchestration service coordinating the deck-build pipeline; decomposition tracked in
// #3.
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.TooManyMethods"})
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
        var finalCards =
                basicLandPadder.pad(
                        DeckDraftPicker.pick(candidates, targetSize), identity, targetSize, gaps);
        return deckBuildRecorder.record(request, commanders, finalCards, gaps);
    }
}
