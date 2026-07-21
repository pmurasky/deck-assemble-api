package com.deckassemble.collections.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private CardCollectionRepository collectionRepository;
    @Mock private CollectionCardRepository collectionCardRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private CardCatalogService cardCatalogService;

    @Test
    void shouldListCollectionsForCurrentProfile() {
        stubUser();
        when(collectionRepository.findByProfileIdOrderByNameAsc(PROFILE_ID))
                .thenReturn(List.of(collection("Alpha"), collection("Beta")));

        List<CollectionResponse> result = service().list();

        assertThat(result).extracting(CollectionResponse::name).containsExactly("Alpha", "Beta");
    }

    @Test
    void shouldCreateCollectionWithDefaultFlagOffWhenNull() {
        stubUser();
        when(collectionRepository.save(any(CardCollection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CollectionResponse result =
                service().create(new CollectionCreateRequest("New", null, null));

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.defaultCollection()).isFalse();
    }

    @Test
    void shouldThrowWhenCollectionNotOwned() {
        stubUser();
        when(collectionRepository.findByIdAndProfileId(1L, PROFILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getById(1L))
                .isInstanceOf(CollectionNotFoundException.class);
    }

    @Test
    void shouldApplyOnlyProvidedFieldsOnUpdate() {
        stubUser();
        CardCollection collection = collection("Original");
        when(collectionRepository.findByIdAndProfileId(1L, PROFILE_ID))
                .thenReturn(Optional.of(collection));
        when(collectionRepository.save(any(CardCollection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CollectionResponse result =
                service().update(1L, new CollectionUpdateRequest("Renamed", null, true));

        assertThat(result.name()).isEqualTo("Renamed");
        assertThat(result.description()).isEqualTo("desc");
        assertThat(result.defaultCollection()).isTrue();
    }

    @Test
    void shouldDeleteOwnedCollection() {
        stubUser();
        CardCollection collection = collection("Alpha");
        when(collectionRepository.findByIdAndProfileId(1L, PROFILE_ID))
                .thenReturn(Optional.of(collection));

        service().delete(1L);

        verify(collectionRepository).delete(collection);
    }

    @Test
    void shouldAddNewCardToCollection() {
        stubUser();
        when(collectionRepository.findByIdAndProfileId(1L, PROFILE_ID))
                .thenReturn(Optional.of(collection("Alpha")));
        when(collectionCardRepository.findByCollectionIdAndCardPrintingId(1L, 10L))
                .thenReturn(Optional.empty());
        when(collectionCardRepository.save(any(CollectionCard.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CollectionCardResponse result =
                service().addCard(1L, new CollectionCardAddRequest(10L, 2, 1));

        assertThat(result.regularQuantity()).isEqualTo(2);
        assertThat(result.foilQuantity()).isEqualTo(1);
    }

    @Test
    void shouldMergeQuantitiesWhenCardAlreadyCollected() {
        stubUser();
        when(collectionRepository.findByIdAndProfileId(1L, PROFILE_ID))
                .thenReturn(Optional.of(collection("Alpha")));
        CollectionCard existing = new CollectionCard(1L, 10L, 2, 1);
        when(collectionCardRepository.findByCollectionIdAndCardPrintingId(1L, 10L))
                .thenReturn(Optional.of(existing));
        when(collectionCardRepository.save(any(CollectionCard.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CollectionCardResponse result =
                service().addCard(1L, new CollectionCardAddRequest(10L, 3, 2));

        assertThat(result.regularQuantity()).isEqualTo(5);
        assertThat(result.foilQuantity()).isEqualTo(3);
    }

    @Test
    void shouldUpdateCardQuantities() {
        stubUser();
        when(collectionRepository.findByIdAndProfileId(1L, PROFILE_ID))
                .thenReturn(Optional.of(collection("Alpha")));
        CollectionCard card = new CollectionCard(1L, 10L, 2, 1);
        when(collectionCardRepository.findByIdAndCollectionId(7L, 1L))
                .thenReturn(Optional.of(card));
        when(collectionCardRepository.save(any(CollectionCard.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CollectionCardResponse result =
                service().updateCard(1L, 7L, new CollectionCardUpdateRequest(9, 0));

        assertThat(result.regularQuantity()).isEqualTo(9);
        assertThat(result.foilQuantity()).isEqualTo(0);
    }

    @Test
    void shouldThrowWhenRemovingCardNotInCollection() {
        stubUser();
        when(collectionRepository.findByIdAndProfileId(1L, PROFILE_ID))
                .thenReturn(Optional.of(collection("Alpha")));
        when(collectionCardRepository.findByIdAndCollectionId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().removeCard(1L, 7L))
                .isInstanceOf(CollectionCardNotFoundException.class);
    }

    @Test
    void shouldRejectUnauthenticatedUser() {
        when(currentUser.subject()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().list()).isInstanceOf(IllegalStateException.class);
    }

    private CollectionService service() {
        return new CollectionService(
                collectionRepository,
                collectionCardRepository,
                currentUser,
                profileService,
                cardCatalogService);
    }

    private void stubUser() {
        Profile profile = new Profile("sub", "User");
        ReflectionTestUtils.setField(profile, "id", PROFILE_ID);
        when(currentUser.subject()).thenReturn(Optional.of("sub"));
        when(profileService.getOrCreate("sub")).thenReturn(profile);
    }

    private CardCollection collection(String name) {
        CardCollection collection = new CardCollection(PROFILE_ID, name, "desc", false);
        ReflectionTestUtils.setField(collection, "id", 1L);
        return collection;
    }
}
