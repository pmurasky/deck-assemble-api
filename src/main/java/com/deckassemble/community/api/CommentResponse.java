package com.deckassemble.community.api;

import com.deckassemble.community.domain.DeckComment;
import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id, long deckId, long profileId, String body, Instant createdAt, Instant updatedAt) {

    public static CommentResponse from(DeckComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getDeckId(),
                comment.getProfileId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
