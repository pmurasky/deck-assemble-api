package com.deckassemble.decks.application.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.community.application.CommunityEvent;
import com.deckassemble.community.domain.Notification.Reason;
import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckCreateRequest;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.application.organization.DeckCategoryService;
import com.deckassemble.decks.application.organization.DeckTagService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit-level coverage for DeckForkService's composition logic (snapshot -> create-request mapping,
 * category/tag/card application, exactly-one-revision discipline, source attribution) with all
 * collaborators mocked. The security-relevant slug-gating and real-persistence behavior (private
 * decks/lingering slugs rejected, attribution survives source deletion/privacy change, live edits
 * not reflected until republish) is covered by DeckPublishingControllerIntegrationTest instead,
 * where a real gated DeckPublishingService and a real database are exercised end to end.
 */
@ExtendWith(MockitoExtension.class)
class DeckForkServiceTest {

    @Mock private DeckPublishingService deckPublishingService;
    @Mock private DeckRepository deckRepository;
    @Mock private DeckService deckService;
    @Mock private DeckCardService deckCardService;
    @Mock private DeckCategoryService deckCategoryService;
    @Mock private DeckTagService deckTagService;
    @Mock private DeckRevisionService deckRevisionService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DeckForkService service() {
        return new DeckForkService(
                deckPublishingService,
                deckRepository,
                deckService,
                deckCardService,
                deckCategoryService,
                deckTagService,
                deckRevisionService,
                eventPublisher);
    }

    @Test
    void shouldRejectForkingADeckThatHasNeverBeenPublished() {
        Deck source = new Deck(9L, "Source", "COMMANDER");
        ReflectionTestUtils.setField(source, "id", 5L);
        when(deckPublishingService.getShared("slug"))
                .thenReturn(new DeckPublishingService.SharedDeckView(source, null));

        assertThatThrownBy(() -> service().fork("slug"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verifyNoInteractions(deckService, deckCardService, deckCategoryService, deckTagService);
        verify(deckRepository, never()).findByShareSlug(any());
        verify(deckRepository, never()).findById(any());
    }

    @Test
    void shouldForkPinnedSnapshotContentIntoANewDeckAndRecordExactlyOneForkedRevision() {
        allowWithoutRecordingToRunTheSuppliedAction();
        Deck source = new Deck(9L, "Source", "COMMANDER");
        ReflectionTestUtils.setField(source, "id", 5L);
        source.setPublishedRevisionNumber(2);
        DeckSnapshot pinned =
                new DeckSnapshot(
                        "Pinned Deck",
                        "COMMANDER",
                        "Pinned description",
                        111L,
                        null,
                        // folderId: belongs to the source owner, must never be applied to the fork.
                        999L,
                        true,
                        BigDecimal.TEN,
                        7,
                        "Aggro",
                        // status: a fresh fork stays DRAFT (DeckService.create's default), not
                        // whatever the source happened to be at publish time.
                        "ARCHIVED",
                        List.of(new DeckSnapshot.CardEntry(501L, 2, "MAIN_DECK", "OWNED")),
                        List.of("Ramp"),
                        List.of("Spicy"));
        when(deckPublishingService.getShared("slug"))
                .thenReturn(new DeckPublishingService.SharedDeckView(source, pinned));
        var requestCaptor = ArgumentCaptor.forClass(DeckCreateRequest.class);
        when(deckService.create(requestCaptor.capture()))
                .thenReturn(deckResponse(99L, "Pinned Deck"));
        when(deckCategoryService.list(99L)).thenReturn(List.of());
        when(deckTagService.list()).thenReturn(List.of(new DeckTagService.TagView(55L, "Spicy")));
        Deck forked99 = new Deck(1L, "Pinned Deck", "COMMANDER");
        ReflectionTestUtils.setField(forked99, "id", 99L);
        when(deckRepository.findById(99L)).thenReturn(Optional.of(forked99));
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));

        Deck result = service().fork("slug");

        assertThat(requestCaptor.getValue())
                .isEqualTo(
                        new DeckCreateRequest(
                                "Pinned Deck",
                                "COMMANDER",
                                "Pinned description",
                                111L,
                                null,
                                true,
                                BigDecimal.TEN,
                                7,
                                "Aggro"));
        verify(deckCardService)
                .addCard(99L, new DeckCardAddRequest(501L, 2, DeckCard.Section.MAIN_DECK, null));
        verify(deckCategoryService).create(99L, "Ramp", null);
        verify(deckTagService).assignToDeck(99L, List.of(55L), null);
        verify(deckRevisionService).record(99L, 1L, DeckChangeType.FORKED);
        verify(deckRevisionService, times(1)).record(anyLong(), anyLong(), any());
        verify(eventPublisher).publishEvent(new CommunityEvent(Reason.DECK_FORKED, 1L, 9L, "5"));
        assertThat(result.getSourceDeckId()).isEqualTo(5L);
        assertThat(result.getSourceRevisionNumber()).isEqualTo(2);
    }

    @Test
    void shouldNotRecreateACategoryTheSnapshotAlreadyHasByName() {
        allowWithoutRecordingToRunTheSuppliedAction();
        Deck source = new Deck(9L, "Source", "COMMANDER");
        ReflectionTestUtils.setField(source, "id", 5L);
        source.setPublishedRevisionNumber(1);
        DeckSnapshot pinned = minimalSnapshotWithCategory("Ramp");
        when(deckPublishingService.getShared("slug"))
                .thenReturn(new DeckPublishingService.SharedDeckView(source, pinned));
        when(deckService.create(any())).thenReturn(deckResponse(99L, "Pinned Deck"));
        when(deckCategoryService.list(99L))
                .thenReturn(
                        List.of(
                                new DeckCategoryService.CategoryView(
                                        1L, "Ramp", 0, true, null, List.of(), 0)));
        when(deckTagService.list()).thenReturn(List.of());
        Deck forked99 = new Deck(1L, "Pinned Deck", "COMMANDER");
        ReflectionTestUtils.setField(forked99, "id", 99L);
        when(deckRepository.findById(99L)).thenReturn(Optional.of(forked99));
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));

        service().fork("slug");

        verify(deckCategoryService, never()).create(eq(99L), any(), any());
    }

    @Test
    void shouldSkipTagAssignmentWhenCallerHasNoTagOfThatNameYet() {
        allowWithoutRecordingToRunTheSuppliedAction();
        Deck source = new Deck(9L, "Source", "COMMANDER");
        ReflectionTestUtils.setField(source, "id", 5L);
        source.setPublishedRevisionNumber(1);
        DeckSnapshot pinned = minimalSnapshotWithTag("Spicy");
        when(deckPublishingService.getShared("slug"))
                .thenReturn(new DeckPublishingService.SharedDeckView(source, pinned));
        when(deckService.create(any())).thenReturn(deckResponse(99L, "Pinned Deck"));
        when(deckCategoryService.list(99L)).thenReturn(List.of());
        when(deckTagService.list()).thenReturn(List.of());
        Deck forked99 = new Deck(1L, "Pinned Deck", "COMMANDER");
        ReflectionTestUtils.setField(forked99, "id", 99L);
        when(deckRepository.findById(99L)).thenReturn(Optional.of(forked99));
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));

        service().fork("slug");

        verify(deckTagService, never()).assignToDeck(anyLong(), any(), any());
    }

    private void allowWithoutRecordingToRunTheSuppliedAction() {
        lenient()
                .when(
                        deckRevisionService.withoutRecording(
                                org.mockito.ArgumentMatchers.<Supplier<Object>>any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    private static DeckSnapshot minimalSnapshotWithCategory(String categoryName) {
        return new DeckSnapshot(
                "Pinned Deck",
                "COMMANDER",
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                "DRAFT",
                List.of(),
                List.of(categoryName),
                List.of());
    }

    private static DeckSnapshot minimalSnapshotWithTag(String tagName) {
        return new DeckSnapshot(
                "Pinned Deck",
                "COMMANDER",
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                "DRAFT",
                List.of(),
                List.of(),
                List.of(tagName));
    }

    private static DeckResponse deckResponse(long id, String name) {
        return new DeckResponse(
                id,
                name,
                "COMMANDER",
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                "DRAFT",
                0,
                null,
                null,
                null,
                0);
    }
}
