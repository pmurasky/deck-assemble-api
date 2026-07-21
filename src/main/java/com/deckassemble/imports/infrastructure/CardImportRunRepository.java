package com.deckassemble.imports.infrastructure;

import com.deckassemble.imports.domain.CardImportRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardImportRunRepository extends JpaRepository<CardImportRun, Long> {

    Optional<CardImportRun> findTopByStatusOrderByCompletedAtDesc(CardImportRun.Status status);

    List<CardImportRun> findTop20ByOrderByStartedAtDesc();
}
