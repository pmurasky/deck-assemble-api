package com.deckassemble.community.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationReportRepository extends JpaRepository<ModerationReport, UUID> {

    List<ModerationReport> findByStatus(ModerationReport.Status status);
}
