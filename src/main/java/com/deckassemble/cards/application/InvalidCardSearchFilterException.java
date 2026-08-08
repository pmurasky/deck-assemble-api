package com.deckassemble.cards.application;

/**
 * Thrown when a typed, allow-listed search filter value is not recognized (e.g. an unknown
 * functional category name).
 */
public class InvalidCardSearchFilterException extends RuntimeException {

    public InvalidCardSearchFilterException(String filterName, String value, Throwable cause) {
        super("Invalid value \"" + value + "\" for filter \"" + filterName + "\".", cause);
    }
}
