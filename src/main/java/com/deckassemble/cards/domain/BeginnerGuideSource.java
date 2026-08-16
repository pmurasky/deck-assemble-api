package com.deckassemble.cards.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Card rules text and rulings supplied to beginner-guide generation. */
public record BeginnerGuideSource(String cardName, List<String> oracleTexts, List<String> rulings) {
    public BeginnerGuideSource {
        oracleTexts = List.copyOf(oracleTexts);
        rulings = List.copyOf(rulings);
    }

    /** Creates the canonical guide source for a card and its current rulings. */
    public static BeginnerGuideSource fromCard(Card card, List<String> rulings) {
        var oracleTexts =
                card.getFaces().isEmpty()
                        ? List.of(card.getOracleText())
                        : card.getFaces().stream().map(CardFace::getOracleText).toList();
        return new BeginnerGuideSource(card.getName(), oracleTexts, rulings);
    }

    public String oracleHash() {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var source = String.join("\n", oracleTexts).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(source));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
