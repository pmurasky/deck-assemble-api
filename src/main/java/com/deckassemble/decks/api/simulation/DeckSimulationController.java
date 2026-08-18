package com.deckassemble.decks.api.simulation;

import com.deckassemble.decks.application.simulation.DeckSampleHandRequest;
import com.deckassemble.decks.application.simulation.DeckSampleHandResponse;
import com.deckassemble.decks.application.simulation.DeckSampleHandService;
import com.deckassemble.decks.application.simulation.DeckSimulationRequest;
import com.deckassemble.decks.application.simulation.DeckSimulationResponse;
import com.deckassemble.decks.application.simulation.DeckSimulationService;
import com.deckassemble.decks.application.simulation.PracticeSessionRequest;
import com.deckassemble.decks.application.simulation.PracticeSessionResponse;
import com.deckassemble.decks.application.simulation.PracticeSessionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generates deterministic, seeded sample opening hands and Monte Carlo consistency simulations from
 * a deck revision's snapshot.
 */
@RestController
@RequestMapping("/decks/{deckId}")
public class DeckSimulationController {

    private final DeckSampleHandService deckSampleHandService;
    private final DeckSimulationService deckSimulationService;
    private final PracticeSessionService practiceSessionService;

    public DeckSimulationController(
            DeckSampleHandService deckSampleHandService,
            DeckSimulationService deckSimulationService,
            PracticeSessionService practiceSessionService) {
        this.deckSampleHandService = deckSampleHandService;
        this.deckSimulationService = deckSimulationService;
        this.practiceSessionService = practiceSessionService;
    }

    @PostMapping("/sample-hands")
    public DeckSampleHandResponse generate(
            @PathVariable long deckId, @Valid @RequestBody DeckSampleHandRequest request) {
        return deckSampleHandService.generate(deckId, request);
    }

    @PostMapping("/simulations")
    public DeckSimulationResponse simulate(
            @PathVariable long deckId, @Valid @RequestBody DeckSimulationRequest request) {
        return deckSimulationService.simulate(deckId, request);
    }

    @PostMapping("/practice-sessions")
    public PracticeSessionResponse startPractice(
            @PathVariable long deckId, @Valid @RequestBody PracticeSessionRequest request) {
        return practiceSessionService.start(deckId, request);
    }

    @PostMapping("/practice-sessions/{sessionId}/steps")
    public PracticeSessionResponse stepPractice(
            @PathVariable long deckId, @PathVariable UUID sessionId) {
        return practiceSessionService.step(deckId, sessionId);
    }

    @PostMapping("/practice-sessions/{sessionId}/reset")
    public PracticeSessionResponse resetPractice(
            @PathVariable long deckId, @PathVariable UUID sessionId) {
        return practiceSessionService.reset(deckId, sessionId);
    }
}
