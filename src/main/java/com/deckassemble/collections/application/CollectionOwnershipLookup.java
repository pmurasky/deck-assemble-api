package com.deckassemble.collections.application;

import com.deckassemble.cards.application.CardOwnershipLookup;
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.users.application.ProfileService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the {@code cards} module's owned-quantity port. Lives here (not in {@code cards})
 * because {@code collections} already depends on {@code cards}; this adapter inverts the dependency
 * so {@code cards} never has to import {@code collections} (see {@link CardOwnershipLookup}).
 * Depends on the collection repositories directly rather than {@link CollectionService}: that
 * service itself depends on {@code CardCatalogService}, and going through it here would close a
 * Spring bean cycle back to the very service this lookup serves.
 */
@Component
public class CollectionOwnershipLookup implements CardOwnershipLookup {

    private final ProfileService profileService;
    private final CardCollectionRepository collectionRepository;
    private final CollectionCardRepository collectionCardRepository;

    public CollectionOwnershipLookup(
            ProfileService profileService,
            CardCollectionRepository collectionRepository,
            CollectionCardRepository collectionCardRepository) {
        this.profileService = profileService;
        this.collectionRepository = collectionRepository;
        this.collectionCardRepository = collectionCardRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> ownedQuantitiesBySubject(String subject) {
        return profileService
                .findBySubject(subject)
                .map(profile -> quantitiesByPrintingId(profile.getId()))
                .orElse(Map.of());
    }

    private Map<Long, Integer> quantitiesByPrintingId(long profileId) {
        List<Long> collectionIds =
                collectionRepository.findByProfileIdOrderByNameAsc(profileId).stream()
                        .map(CardCollection::getId)
                        .toList();
        if (collectionIds.isEmpty()) {
            return Map.of();
        }
        return collectionCardRepository.findByCollectionIdIn(collectionIds).stream()
                .collect(
                        Collectors.groupingBy(
                                CollectionCard::getCardPrintingId,
                                Collectors.summingInt(
                                        card ->
                                                card.getRegularQuantity()
                                                        + card.getFoilQuantity())));
    }
}
