package com.deckassemble.cards.application;

public class FinishUnavailableException extends RuntimeException {

    public FinishUnavailableException(String finish) {
        super("Printing is not available in " + finish + " finish.");
    }
}
