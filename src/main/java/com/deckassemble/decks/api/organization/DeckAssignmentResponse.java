package com.deckassemble.decks.api.organization;

/**
 * Response for the folder/tag assignment endpoints, which have no other resource body to carry the
 * resulting revision on (unlike category responses, which already return the category).
 */
public record DeckAssignmentResponse(int revisionNumber) {}
