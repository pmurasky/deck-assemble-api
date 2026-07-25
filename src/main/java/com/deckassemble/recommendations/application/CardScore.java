package com.deckassemble.recommendations.application;

import org.jspecify.annotations.Nullable;

public record CardScore(@Nullable Double synergy, @Nullable Long inclusion) {}
