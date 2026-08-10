package com.deckassemble.community.api;

import com.deckassemble.community.application.CommentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comments on a shared deck, addressed by slug (see {@code DeckPublishingService#getShared} for
 * why: it's the single visibility gate every shared-deck access goes through). Listing is
 * anonymous-reachable, same as the shared deck view itself (see SecurityConfig's permitAll on GET
 * {@code /shared/decks/**}); create/edit/delete require authentication.
 */
@RestController
@RequestMapping("/shared/decks/{slug}/comments")
public class CommunityCommentController {

    private final CommentService commentService;

    public CommunityCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public Page<CommentResponse> list(
            @PathVariable String slug, @PageableDefault(size = 20) Pageable pageable) {
        return commentService.list(slug, pageable).map(CommentResponse::from);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable String slug, @Valid @RequestBody CommentRequest request) {
        CommentResponse response =
                CommentResponse.from(commentService.create(slug, request.body()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{commentId}")
    public CommentResponse edit(
            @PathVariable String slug,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request) {
        return CommentResponse.from(commentService.edit(slug, commentId, request.body()));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable String slug, @PathVariable UUID commentId) {
        commentService.delete(slug, commentId);
        return ResponseEntity.noContent().build();
    }
}
