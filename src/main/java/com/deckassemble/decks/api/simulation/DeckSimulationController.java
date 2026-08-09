package com.deckassemble.decks.api.simulation;

import com.deckassemble.decks.application.simulation.DeckSampleHandRequest;
import com.deckassemble.decks.application.simulation.DeckSampleHandResponse;
import com.deckassemble.decks.application.simulation.DeckSampleHandService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Generates deterministic, seeded sample opening hands from a deck revision's snapshot. */
@RestController
@RequestMapping("/decks/{deckId}/sample-hands")
public class DeckSimulationController {

    private final DeckSampleHandService deckSampleHandService;

    public DeckSimulationController(DeckSampleHandService deckSampleHandService) {
        this.deckSampleHandService = deckSampleHandService;
    }

    @PostMapping
    public DeckSampleHandResponse generate(
            @PathVariable long deckId, @Valid @RequestBody DeckSampleHandRequest request) {
        return deckSampleHandService.generate(deckId, request);
    }
}
