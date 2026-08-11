package com.deckassemble.collections.domain.trading;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeListRepository extends JpaRepository<TradeList, Long> {

    List<TradeList> findByProfileIdOrderById(Long profileId);

    Optional<TradeList> findByIdAndProfileId(Long id, Long profileId);
}
