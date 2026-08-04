package com.deckassemble.cards.application;

import org.jspecify.annotations.Nullable;

record CardSearchFilter(
        String query,
        @Nullable String setCode,
        @Nullable String colorIdentity,
        @Nullable String type,
        @Nullable Boolean commanderEligible) {}
