package com.deckassemble.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.community.domain.DeckComment;
import com.deckassemble.community.domain.DeckCommentRepository;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.publishing.DeckPublishingService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private DeckCommentRepository deckCommentRepository;
    @Mock private DeckPublishingService deckPublishingService;
    @Mock private DeckAccessGuard deckAccessGuard;

    private CommentService service;

    @BeforeEach
    void setUp() {
        service = new CommentService(deckCommentRepository, deckPublishingService, deckAccessGuard);
    }

    private Deck publishedDeck(long deckId, long ownerId) {
        Deck deck = new Deck(ownerId, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", deckId);
        deck.setVisibility(DeckVisibility.PUBLIC);
        deck.setShareSlug("slug");
        deck.setPublishedRevisionNumber(1);
        return deck;
    }

    private void stubShared(String slug, Deck deck) {
        when(deckPublishingService.getShared(slug))
                .thenReturn(new DeckPublishingService.SharedDeckView(deck, null));
    }

    @Test
    void shouldListNonDeletedCommentsForTheDeckBehindTheSlug() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        Page<DeckComment> page = new PageImpl<>(java.util.List.of());
        when(deckCommentRepository.findByDeckIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        7L, PageRequest.of(0, 20)))
                .thenReturn(page);

        Page<DeckComment> result = service.list("slug", PageRequest.of(0, 20));

        assertThat(result).isSameAs(page);
    }

    @Test
    void shouldCreateACommentAgainstAPublishedVisibleDeck() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        when(deckAccessGuard.profileId()).thenReturn(5L);
        when(deckCommentRepository.countByProfileIdAndCreatedAtAfter(anyLong(), any(Instant.class)))
                .thenReturn(0L);
        when(deckCommentRepository.save(any(DeckComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeckComment created = service.create("slug", "Nice deck!");

        assertThat(created.getDeckId()).isEqualTo(7L);
        assertThat(created.getProfileId()).isEqualTo(5L);
        assertThat(created.getBody()).isEqualTo("Nice deck!");
    }

    @Test
    void shouldRejectCreatingACommentOnADeckThatHasNeverBeenPublished() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", 7L);
        deck.setVisibility(DeckVisibility.PUBLIC);
        stubShared("slug", deck);

        assertThatThrownBy(() -> service.create("slug", "Nice deck!"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("has not been published");
        verify(deckCommentRepository, never()).save(any(DeckComment.class));
    }

    @Test
    void shouldRejectCreatingACommentWhenTheOwnerDisabledComments() {
        Deck deck = publishedDeck(7L, 1L);
        deck.setCommentsEnabled(false);
        stubShared("slug", deck);

        assertThatThrownBy(() -> service.create("slug", "Nice deck!"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("disabled");
        verify(deckCommentRepository, never()).save(any(DeckComment.class));
    }

    @Test
    void shouldRejectCreatingACommentWhenTheProfileHasHitTheRateLimit() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        when(deckAccessGuard.profileId()).thenReturn(5L);
        when(deckCommentRepository.countByProfileIdAndCreatedAtAfter(anyLong(), any(Instant.class)))
                .thenReturn((long) CommentService.MAX_COMMENTS_PER_WINDOW);

        assertThatThrownBy(() -> service.create("slug", "Spam"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many comments");
        verify(deckCommentRepository, never()).save(any(DeckComment.class));
    }

    @Test
    void shouldLetTheAuthorEditTheirOwnComment() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        UUID commentId = UUID.randomUUID();
        DeckComment comment = new DeckComment(7L, 5L, "Original");
        when(deckCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(deckAccessGuard.profileId()).thenReturn(5L);
        when(deckCommentRepository.save(any(DeckComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeckComment edited = service.edit("slug", commentId, "Edited");

        assertThat(edited.getBody()).isEqualTo("Edited");
    }

    @Test
    void shouldRejectEditingSomeoneElsesComment() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        UUID commentId = UUID.randomUUID();
        DeckComment comment = new DeckComment(7L, 5L, "Original");
        when(deckCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(deckAccessGuard.profileId()).thenReturn(99L);

        assertThatThrownBy(() -> service.edit("slug", commentId, "Edited"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("author");
        verify(deckCommentRepository, never()).save(any(DeckComment.class));
    }

    @Test
    void shouldReturnNotFoundWhenEditingAnUnknownComment() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        UUID commentId = UUID.randomUUID();
        when(deckCommentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.edit("slug", commentId, "Edited"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldReturnNotFoundWhenEditingAlreadySoftDeletedComment() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        UUID commentId = UUID.randomUUID();
        DeckComment comment = new DeckComment(7L, 5L, "Original");
        comment.softDelete();
        when(deckCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.edit("slug", commentId, "Edited"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldLetTheAuthorDeleteTheirOwnComment() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        UUID commentId = UUID.randomUUID();
        DeckComment comment = new DeckComment(7L, 5L, "Original");
        when(deckCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(deckAccessGuard.profileId()).thenReturn(5L);
        when(deckCommentRepository.save(any(DeckComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.delete("slug", commentId);

        assertThat(comment.isDeleted()).isTrue();
        verify(deckCommentRepository).save(comment);
    }

    @Test
    void shouldLetTheDeckOwnerModerateAndDeleteSomeoneElsesComment() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        UUID commentId = UUID.randomUUID();
        DeckComment comment = new DeckComment(7L, 5L, "Original");
        when(deckCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(deckAccessGuard.profileId()).thenReturn(1L); // deck owner, not the comment's author
        when(deckCommentRepository.save(any(DeckComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.delete("slug", commentId);

        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    void shouldRejectAStrangerDeletingSomeoneElsesComment() {
        Deck deck = publishedDeck(7L, 1L);
        stubShared("slug", deck);
        UUID commentId = UUID.randomUUID();
        DeckComment comment = new DeckComment(7L, 5L, "Original");
        when(deckCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(deckAccessGuard.profileId()).thenReturn(99L);

        assertThatThrownBy(() -> service.delete("slug", commentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("author or the deck owner");
        assertThat(comment.isDeleted()).isFalse();
        verify(deckCommentRepository, never()).save(any(DeckComment.class));
    }
}
