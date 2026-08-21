package com.deckassemble.imports.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Supported card series for checkbox-driven Scryfall imports. Set codes verified against Scryfall's
 * Sets API.
 */
public enum CardSeries {
    MARVEL("Marvel", List.of("mar", "msh", "msc", "spm", "spe")),
    SPIDER_MAN("Spider-Man", List.of("spm", "spe")),
    HOBBIT("The Hobbit", List.of("hob", "hoc")),
    TMNT("Teenage Mutant Ninja Turtles", List.of("tmt", "tmc")),
    ASSASSINS_CREED("Assassin's Creed", List.of("acr")),
    LORWYN_ECLIPSED("Lorwyn Eclipsed", List.of("ecl", "ecc", "spg")),
    AETHERDRIFT("Aetherdrift", List.of("dft", "drc")),
    ZENDIKAR_RISING("Zendikar Rising", List.of("znr")),
    TALES_OF_MIDDLE_EARTH_COMMANDER("Tales of Middle-earth Commander", List.of("ltc")),
    FOUNDATIONS("Foundations", List.of("fdn")),
    BLOOMBURROW("Bloomburrow", List.of("blb")),
    SECRET_LAIR_DROP("Secret Lair Drop", List.of("sld"));

    private final String label;
    private final List<String> setCodes;

    CardSeries(String label, List<String> setCodes) {
        this.label = label;
        this.setCodes = setCodes;
    }

    public String label() {
        return label;
    }

    public List<String> setCodes() {
        return setCodes;
    }

    /** Resolves a stable series key (case-insensitive) to its enum value. */
    public static Optional<CardSeries> fromKey(String key) {
        return Arrays.stream(values())
                .filter(series -> series.name().equalsIgnoreCase(key.trim()))
                .findFirst();
    }

    /**
     * Combines series selections into one Scryfall set-code query fragment, e.g. {@code e:hob,hoc}.
     */
    public static String toQueryFragment(List<CardSeries> series) {
        if (series.isEmpty()) {
            throw new IllegalArgumentException("At least one card series is required");
        }
        return series.stream()
                .flatMap(selected -> selected.setCodes.stream())
                .distinct()
                .collect(Collectors.joining(",", "e:", ""));
    }
}
