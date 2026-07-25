package com.deckassemble.recommendations.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdhrecCommanderCacheRepository extends JpaRepository<EdhrecCommanderCache, Long> {

    Optional<EdhrecCommanderCache> findByCommanderOracleId(String commanderOracleId);
}
