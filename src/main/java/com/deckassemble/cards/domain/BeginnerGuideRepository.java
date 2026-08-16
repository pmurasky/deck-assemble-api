package com.deckassemble.cards.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access for one beginner guide per card. */
public interface BeginnerGuideRepository extends JpaRepository<BeginnerGuide, Long> {}
