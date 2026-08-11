package com.deckassemble.collections.application.trading;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.domain.trading.TradeList;
import com.deckassemble.collections.domain.trading.TradeListRepository;
import com.deckassemble.collections.domain.trading.TradeListVisibility;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class TradeListVisibilityGuard {

    private final CollectionAccessGuard accessGuard;
    private final TradeListRepository listRepository;

    TradeListVisibilityGuard(
            CollectionAccessGuard accessGuard, TradeListRepository listRepository) {
        this.accessGuard = accessGuard;
        this.listRepository = listRepository;
    }

    VisibleLists visible(long leftListId, long rightListId) {
        long requesterProfileId = accessGuard.profileId();
        return new VisibleLists(
                visibleList(leftListId, requesterProfileId),
                visibleList(rightListId, requesterProfileId));
    }

    private TradeList visibleList(long id, long requesterProfileId) {
        TradeList list =
                listRepository
                        .findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (list.getProfileId().equals(requesterProfileId)
                || list.getVisibility() != TradeListVisibility.PRIVATE) {
            return list;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    record VisibleLists(TradeList left, TradeList right) {}
}
