package com.deckassemble.collections.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.collections.api.CollectionCardAddRequest;
import com.deckassemble.collections.api.CollectionCardResponse;
import com.deckassemble.collections.api.CollectionCardUpdateRequest;
import com.deckassemble.collections.api.CollectionCreateRequest;
import com.deckassemble.collections.api.CollectionResponse;
import com.deckassemble.collections.api.CollectionUpdateRequest;
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.infrastructure.CardCollectionRepository;
import com.deckassemble.collections.infrastructure.CollectionCardRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CollectionService {

  private final CardCollectionRepository collectionRepository;
  private final CollectionCardRepository collectionCardRepository;
  private final CurrentUser currentUser;
  private final ProfileService profileService;
  private final CardCatalogService cardCatalogService;

  public CollectionService(
      CardCollectionRepository collectionRepository,
      CollectionCardRepository collectionCardRepository,
      CurrentUser currentUser,
      ProfileService profileService,
      CardCatalogService cardCatalogService) {
    this.collectionRepository = collectionRepository;
    this.collectionCardRepository = collectionCardRepository;
    this.currentUser = currentUser;
    this.profileService = profileService;
    this.cardCatalogService = cardCatalogService;
  }

  public List<CollectionResponse> list() {
    return collectionRepository.findByProfileIdOrderByNameAsc(profileId()).stream()
        .map(CollectionResponse::from)
        .toList();
  }

  public CollectionResponse create(CollectionCreateRequest request) {
    CardCollection collection =
        new CardCollection(
            profileId(),
            request.name(),
            request.description(),
            Boolean.TRUE.equals(request.defaultCollection()));
    return CollectionResponse.from(collectionRepository.save(collection));
  }

  public CollectionResponse getById(long collectionId) {
    return CollectionResponse.from(owned(collectionId));
  }

  public CollectionResponse update(long collectionId, CollectionUpdateRequest request) {
    CardCollection collection = owned(collectionId);
    if (request.name() != null) {
      collection.setName(request.name());
    }
    if (request.description() != null) {
      collection.setDescription(request.description());
    }
    if (request.defaultCollection() != null) {
      collection.setDefaultCollection(request.defaultCollection());
    }
    return CollectionResponse.from(collectionRepository.save(collection));
  }

  public void delete(long collectionId) {
    collectionRepository.delete(owned(collectionId));
  }

  public List<CollectionCardResponse> listCards(long collectionId) {
    owned(collectionId);
    return collectionCardRepository.findByCollectionId(collectionId).stream()
        .map(this::responseFor)
        .toList();
  }

  public CollectionCardResponse addCard(long collectionId, CollectionCardAddRequest request) {
    owned(collectionId);
    CollectionCard card =
        collectionCardRepository
            .findByCollectionIdAndCardPrintingId(collectionId, request.cardPrintingId())
            .map(
                existing -> {
                  existing.setRegularQuantity(
                      existing.getRegularQuantity() + request.regularQuantity());
                  existing.setFoilQuantity(existing.getFoilQuantity() + request.foilQuantity());
                  return existing;
                })
            .orElseGet(
                () ->
                    new CollectionCard(
                        collectionId,
                        request.cardPrintingId(),
                        request.regularQuantity(),
                        request.foilQuantity()));
    return responseFor(collectionCardRepository.save(card));
  }

  public CollectionCardResponse updateCard(
      long collectionId, long collectionCardId, CollectionCardUpdateRequest request) {
    owned(collectionId);
    CollectionCard card = ownedCard(collectionId, collectionCardId);
    card.setRegularQuantity(request.regularQuantity());
    card.setFoilQuantity(request.foilQuantity());
    return responseFor(collectionCardRepository.save(card));
  }

  public void removeCard(long collectionId, long collectionCardId) {
    owned(collectionId);
    collectionCardRepository.delete(ownedCard(collectionId, collectionCardId));
  }

  private CardCollection owned(long collectionId) {
    return collectionRepository
        .findByIdAndProfileId(collectionId, profileId())
        .orElseThrow(CollectionNotFoundException::new);
  }

  private CollectionCard ownedCard(long collectionId, long collectionCardId) {
    return collectionCardRepository
        .findByIdAndCollectionId(collectionCardId, collectionId)
        .orElseThrow(CollectionCardNotFoundException::new);
  }

  private Long profileId() {
    String subject =
        currentUser.subject().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    return profileService.getOrCreate(subject).getId();
  }

  private CollectionCardResponse responseFor(CollectionCard card) {
    return CollectionCardResponse.from(
        card, cardCatalogService.getSummaryByPrintingId(card.getCardPrintingId()));
  }
}
