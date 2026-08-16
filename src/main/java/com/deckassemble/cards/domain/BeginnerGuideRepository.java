package com.deckassemble.cards.domain;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence access for one beginner guide per card. */
public interface BeginnerGuideRepository extends JpaRepository<BeginnerGuide, Long> {
    @Query(
            """
            select count(guide) from BeginnerGuide guide
            where guide.generatedBy = :generatedBy
              and guide.generatedAt >= :from
              and guide.generatedAt < :to
            """)
    long countGeneratedByBetween(
            @Param("generatedBy") String generatedBy,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
