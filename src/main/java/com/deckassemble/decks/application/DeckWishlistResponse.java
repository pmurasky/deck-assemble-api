package com.deckassemble.decks.application;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record DeckWishlistResponse(List<DeckWishlistItem> items, @Nullable BigDecimal totalUsd) {}
