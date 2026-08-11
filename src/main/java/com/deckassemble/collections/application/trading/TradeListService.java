package com.deckassemble.collections.application.trading;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import com.deckassemble.collections.domain.trading.TradeList;
import com.deckassemble.collections.domain.trading.TradeListItem;
import com.deckassemble.collections.domain.trading.TradeListItemRepository;
import com.deckassemble.collections.domain.trading.TradeListRepository;
import com.deckassemble.collections.domain.trading.TradeListType;
import com.deckassemble.collections.domain.trading.TradeListVisibility;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TradeListService {

    private final CollectionAccessGuard accessGuard;
    private final TradeListRepository tradeListRepository;
    private final TradeListItemRepository itemRepository;

    public TradeListService(
            CollectionAccessGuard accessGuard,
            TradeListRepository tradeListRepository,
            TradeListItemRepository itemRepository) {
        this.accessGuard = accessGuard;
        this.tradeListRepository = tradeListRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<TradeListView> list() {
        long profileId = accessGuard.profileId();
        return tradeListRepository.findByProfileIdOrderById(profileId).stream()
                .map(list -> view(list, itemRepository.findByTradeListIdOrderById(list.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TradeListView get(long id) {
        TradeList list = owned(id);
        return view(list, itemRepository.findByTradeListIdOrderById(id));
    }

    public TradeListView create(TradeListCommand command) {
        long profileId = accessGuard.profileId();
        validate(command);
        TradeList list =
                tradeListRepository.save(
                        new TradeList(
                                profileId, command.type(), command.name(), command.visibility()));
        List<TradeListItem> items = saveItems(list.getId(), command.items());
        return view(list, items);
    }

    public TradeListView update(long id, TradeListCommand command) {
        validate(command);
        TradeList list = owned(id);
        list.update(command.type(), command.name(), command.visibility());
        itemRepository.deleteByTradeListId(id);
        List<TradeListItem> items = saveItems(id, command.items());
        return view(tradeListRepository.save(list), items);
    }

    public void delete(long id) {
        TradeList list = owned(id);
        itemRepository.deleteByTradeListId(id);
        tradeListRepository.delete(list);
    }

    private TradeList owned(long id) {
        return tradeListRepository
                .findByIdAndProfileId(id, accessGuard.profileId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private List<TradeListItem> saveItems(Long listId, List<TradeListItemCommand> items) {
        return items.stream().map(item -> itemRepository.save(entity(listId, item))).toList();
    }

    private TradeListItem entity(Long listId, TradeListItemCommand item) {
        return new TradeListItem(
                listId,
                item.cardPrintingId(),
                item.quantity(),
                item.condition(),
                item.finish(),
                item.language());
    }

    private void validate(TradeListCommand command) {
        if (command.items().stream().anyMatch(item -> item.quantity() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive.");
        }
    }

    private TradeListView view(TradeList list, List<TradeListItem> items) {
        return new TradeListView(
                list.getId(),
                list.getProfileId(),
                list.getName(),
                list.getType(),
                list.getVisibility(),
                items.stream().map(TradeListService::itemView).toList());
    }

    private static TradeListItemView itemView(TradeListItem item) {
        return new TradeListItemView(
                item.getId(),
                item.getCardPrintingId(),
                item.getQuantity(),
                item.getCondition(),
                item.getFinish(),
                item.getLanguage());
    }

    public record TradeListCommand(
            String name,
            TradeListType type,
            TradeListVisibility visibility,
            List<TradeListItemCommand> items) {}

    public record TradeListItemCommand(
            Long cardPrintingId,
            int quantity,
            @Nullable CardCondition condition,
            @Nullable PhysicalFinish finish,
            @Nullable String language) {}

    public record TradeListView(
            @Nullable Long id,
            Long profileId,
            String name,
            TradeListType type,
            TradeListVisibility visibility,
            List<TradeListItemView> items) {}

    public record TradeListItemView(
            @Nullable Long id,
            Long cardPrintingId,
            int quantity,
            @Nullable CardCondition condition,
            @Nullable PhysicalFinish finish,
            @Nullable String language) {}
}
