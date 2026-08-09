package com.deckassemble.decks.api.simulation;

import com.deckassemble.decks.application.simulation.DeckSampleHandRequest;
import com.deckassemble.decks.application.simulation.DeckSampleHandResponse;
import com.deckassemble.decks.application.simulation.DeckSampleHandService;
import com.deckassemble.decks.application.simulation.DeckSimulationRequest;
import com.deckassemble.decks.application.simulation.DeckSimulationResponse;
import com.deckassemble.decks.application.simulation.DeckSimulationService;
import jakarta.validation.Valid;
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

    public DeckSimulationController(
            DeckSampleHandService deckSampleHandService,
            DeckSimulationService deckSimulationService) {
        this.deckSampleHandService = deckSampleHandService;
        this.deckSimulationService = deckSimulationService;
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
}
