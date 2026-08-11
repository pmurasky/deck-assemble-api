package com.deckassemble.collections.application.trading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.domain.trading.TradeList;
import com.deckassemble.collections.domain.trading.TradeListItem;
import com.deckassemble.collections.domain.trading.TradeListItemRepository;
import com.deckassemble.collections.domain.trading.TradeListRepository;
import com.deckassemble.collections.domain.trading.TradeListType;
import com.deckassemble.collections.domain.trading.TradeListVisibility;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TradeListServiceTest {

    @Mock private CollectionAccessGuard accessGuard;
    @Mock private TradeListRepository tradeListRepository;
    @Mock private TradeListItemRepository itemRepository;

    @Test
    void shouldCreateOwnedTradeListWithItems() {
        when(accessGuard.profileId()).thenReturn(7L);
        when(tradeListRepository.save(any(TradeList.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepository.save(any(TradeListItem.class))).thenAnswer(inv -> inv.getArgument(0));

        TradeListService.TradeListView result =
                service()
                        .create(
                                new TradeListService.TradeListCommand(
                                        "Binder",
                                        TradeListType.OFFERED,
                                        TradeListVisibility.PUBLIC,
                                        List.of(item(11L, 2))));

        assertThat(result.name()).isEqualTo("Binder");
        assertThat(result.profileId()).isEqualTo(7L);
        assertThat(result.items())
                .singleElement()
                .satisfies(item -> assertThat(item.quantity()).isEqualTo(2));
    }

    @Test
    void shouldRejectNonPositiveItemQuantities() {
        when(accessGuard.profileId()).thenReturn(7L);

        assertThatThrownBy(
                        () ->
                                service()
                                        .create(
                                                new TradeListService.TradeListCommand(
                                                        "Bad",
                                                        TradeListType.WANTED,
                                                        TradeListVisibility.PRIVATE,
                                                        List.of(item(11L, 0)))))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        exception ->
                                assertThat(((ResponseStatusException) exception).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldHideListOwnedByAnotherProfile() {
        when(accessGuard.profileId()).thenReturn(7L);
        when(tradeListRepository.findByIdAndProfileId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(99L, command("Hidden")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        exception ->
                                assertThat(((ResponseStatusException) exception).getStatusCode())
                                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void shouldDeleteOwnedListItemsBeforeList() {
        when(accessGuard.profileId()).thenReturn(7L);
        TradeList list =
                new TradeList(7L, TradeListType.OFFERED, "Old", TradeListVisibility.PRIVATE);
        when(tradeListRepository.findByIdAndProfileId(5L, 7L)).thenReturn(Optional.of(list));

        service().delete(5L);

        verify(itemRepository).deleteByTradeListId(5L);
        verify(tradeListRepository).delete(list);
    }

    private TradeListService service() {
        return new TradeListService(accessGuard, tradeListRepository, itemRepository);
    }

    private TradeListService.TradeListCommand command(String name) {
        return new TradeListService.TradeListCommand(
                name, TradeListType.OFFERED, TradeListVisibility.PRIVATE, List.of(item(11L, 1)));
    }

    private TradeListService.TradeListItemCommand item(long printingId, int quantity) {
        return new TradeListService.TradeListItemCommand(printingId, quantity, null, null, null);
    }
}
