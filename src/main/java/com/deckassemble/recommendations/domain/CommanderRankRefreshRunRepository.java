package com.deckassemble.recommendations.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommanderRankRefreshRunRepository extends JpaRepository<CommanderRankRefreshRun, Long> {

    Optional<CommanderRankRefreshRun> findTopByStatusOrderByCompletedAtDesc(CommanderRankRefreshRun.Status status);
}
