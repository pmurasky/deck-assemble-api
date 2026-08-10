package com.deckassemble.community.application;

import com.deckassemble.community.domain.DeckComment;
import com.deckassemble.community.domain.DeckCommentRepository;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.publishing.DeckPublishingService;
import com.deckassemble.decks.domain.Deck;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Comments on a shared deck's published content. Every operation resolves the deck through {@link
 * DeckPublishingService#getShared}, the same visibility gate every other shared-deck access (view,
 * fork) goes through, so comments only ever exist against decks a stranger could actually see; a
 * hidden or unknown slug 404s the same way {@code getShared} already does, without distinguishing
 * "doesn't exist" from "exists but is private".
 *
 * <p>Comments are pinned to a published deck in the loosest schema-consistent sense: creation is
 * rejected until the deck has been published at least once ({@code publishedRevisionNumber != null}
 * — see {@code DeckPublishingService#publish}); there is no per-comment revision column.
 */
@Service
@Transactional
public class CommentService {

    // ponytail: fixed constants, not configuration — no requirement calls for tuning these per
    // environment. Bump/parameterize if that changes.
    static final int MAX_COMMENTS_PER_WINDOW = 5;
    static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final DeckCommentRepository deckCommentRepository;
    private final DeckPublishingService deckPublishingService;
    private final DeckAccessGuard deckAccessGuard;

    public CommentService(
            DeckCommentRepository deckCommentRepository,
            DeckPublishingService deckPublishingService,
            DeckAccessGuard deckAccessGuard) {
        this.deckCommentRepository = deckCommentRepository;
        this.deckPublishingService = deckPublishingService;
        this.deckAccessGuard = deckAccessGuard;
    }

    /** Paginated, most-recent-first, non-deleted comments on a visible shared deck. */
    public Page<DeckComment> list(String slug, Pageable pageable) {
        Deck deck = deckPublishingService.getShared(slug).deck();
        return deckCommentRepository.findByDeckIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                deck.getId(), pageable);
    }

    public DeckComment create(String slug, String body) {
        Deck deck = deckPublishingService.getShared(slug).deck();
        if (deck.getPublishedRevisionNumber() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Deck has not been published yet");
        }
        if (!deck.isCommentsEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Comments are disabled for this deck");
        }
        long profileId = deckAccessGuard.profileId();
        enforceRateLimit(profileId);
        return deckCommentRepository.save(new DeckComment(deck.getId(), profileId, body));
    }

    public DeckComment edit(String slug, UUID commentId, String body) {
        DeckComment comment = visibleCommentOn(slug, commentId);
        long profileId = deckAccessGuard.profileId();
        if (comment.getProfileId() != profileId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the comment's author can edit it");
        }
        comment.editBody(body);
        return deckCommentRepository.save(comment);
    }

    public void delete(String slug, UUID commentId) {
        Deck deck = deckPublishingService.getShared(slug).deck();
        DeckComment comment = visibleComment(deck.getId(), commentId);
        long profileId = deckAccessGuard.profileId();
        boolean isAuthor = comment.getProfileId() == profileId;
        boolean isDeckOwner = deck.getProfileId() == profileId;
        if (!isAuthor && !isDeckOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the comment's author or the deck owner can delete it");
        }
        comment.softDelete();
        deckCommentRepository.save(comment);
    }

    private void enforceRateLimit(long profileId) {
        Instant since = Instant.now().minus(RATE_LIMIT_WINDOW);
        long recent = deckCommentRepository.countByProfileIdAndCreatedAtAfter(profileId, since);
        if (recent >= MAX_COMMENTS_PER_WINDOW) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many comments; wait before posting another");
        }
    }

    private DeckComment visibleCommentOn(String slug, UUID commentId) {
        Deck deck = deckPublishingService.getShared(slug).deck();
        return visibleComment(deck.getId(), commentId);
    }

    private DeckComment visibleComment(long deckId, UUID commentId) {
        DeckComment comment =
                deckCommentRepository
                        .findById(commentId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Comment not found"));
        if (comment.getDeckId() != deckId || comment.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }
        return comment;
    }
}
