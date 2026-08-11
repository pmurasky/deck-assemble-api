package com.deckassemble.collections.domain.trading;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeListItemRepository extends JpaRepository<TradeListItem, Long> {

    List<TradeListItem> findByTradeListIdOrderById(Long tradeListId);

    List<TradeListItem> findByTradeListIdInOrderById(Collection<Long> tradeListIds);

    void deleteByTradeListId(Long tradeListId);
}
