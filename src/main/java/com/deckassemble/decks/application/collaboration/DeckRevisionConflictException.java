package com.deckassemble.decks.application.collaboration;

/**
 * Raised when a collaborator's mutation carries an {@code expectedRevision} that no longer matches
 * the deck's current revision — i.e. someone else's edit landed first. Carries the current revision
 * so the client can rebase and retry. Mapped to HTTP 409 by {@code ApiExceptionHandler}.
 */
public class DeckRevisionConflictException extends RuntimeException {

    private final int currentRevision;

    public DeckRevisionConflictException(int currentRevision) {
        super("Deck was modified concurrently; current revision is " + currentRevision);
        this.currentRevision = currentRevision;
    }

    public int currentRevision() {
        return currentRevision;
    }
}
