package com.deckassemble.cards.application;

import java.util.Map;

/**
 * Port for looking up owned card-printing quantities, implemented in the {@code collections}
 * module. Defined here rather than called directly on {@code collections} because {@code
 * collections} already depends on {@code cards} (e.g. {@code CollectionService} uses {@code
 * CardCatalogService}); a direct {@code cards -> collections} call would create a bounded-context
 * cycle (see {@code ArchitectureTest#bounded_contexts_are_cycle_free}). {@code collections}
 * supplies the Spring bean that implements this interface, inverting the dependency.
 */
public interface CardOwnershipLookup {

    /** Every owned card-printing quantity (regular + foil) for the given auth subject. */
    Map<Long, Integer> ownedQuantitiesBySubject(String subject);
}
