package com.deckassemble.cards.domain;

/** Generates beginner guidance from authoritative card source material. */
public interface BeginnerGuideGenerator {
    BeginnerGuideContent generate(BeginnerGuideSource source);
}
