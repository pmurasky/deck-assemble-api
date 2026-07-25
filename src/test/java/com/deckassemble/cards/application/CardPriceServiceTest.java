package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CardPriceSnapshot;
import com.deckassemble.cards.domain.CardPriceSnapshotRepository;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.ScryfallClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class CardPriceServiceTest {

    @Mock private CardPrintingRepository cardPrintingRepository;
    @Mock private CardPriceSnapshotRepository snapshotRepository;
    @Mock private ScryfallClient scryfallClient;
    @InjectMocks private CardPriceService cardPriceService;

    @Test
    void shouldSaveSnapshotForEachPrinting() {
        var first = printing(1L, "sc-1");
        var second = printing(2L, "sc-2");
        when(cardPrintingRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(first, second));
        when(scryfallClient.getCardPrice("sc-1")).thenReturn(price("1.00", null, "0.90", null));
        when(scryfallClient.getCardPrice("sc-2")).thenReturn(price("2.50", "5.00", null, null));

        var refreshed = cardPriceService.refreshPrices(List.of(1L, 2L));

        assertThat(refreshed).isEqualTo(2);
        var captor = ArgumentCaptor.forClass(CardPriceSnapshot.class);
        verify(snapshotRepository, times(2)).save(captor.capture());
        var snapshots = captor.getAllValues();
        assertThat(snapshots.get(0).getCardPrintingId()).isEqualTo(1L);
        assertThat(snapshots.get(0).getFetchedAt()).isNotNull();
        assertThat(snapshots.get(0).toPrice().usd()).isEqualByComparingTo("1.00");
        assertThat(snapshots.get(0).toPrice().usdFoil()).isNull();
        assertThat(snapshots.get(1).getCardPrintingId()).isEqualTo(2L);
        assertThat(snapshots.get(1).toPrice().usdFoil()).isEqualByComparingTo("5.00");
    }

    @Test
    void shouldContinueRefreshingWhenOnePrintingFails() {
        var failing = printing(1L, "sc-fail");
        var working = printing(2L, "sc-ok");
        when(cardPrintingRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(failing, working));
        when(scryfallClient.getCardPrice("sc-fail")).thenThrow(new RestClientException("boom"));
        when(scryfallClient.getCardPrice("sc-ok")).thenReturn(price("1.00", null, null, null));

        var refreshed = cardPriceService.refreshPrices(List.of(1L, 2L));

        assertThat(refreshed).isEqualTo(1);
        var captor = ArgumentCaptor.forClass(CardPriceSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getCardPrintingId()).isEqualTo(2L);
    }

    @Test
    void shouldDoNothingWhenNoPrintingsGiven() {
        var refreshed = cardPriceService.refreshPrices(List.of());

        assertThat(refreshed).isZero();
        verifyNoInteractions(cardPrintingRepository, snapshotRepository, scryfallClient);
    }

    @Test
    void shouldReturnLatestPricesMappedByPrintingId() {
        var snapshot = new CardPriceSnapshot(7L, price("3.25", null, "2.90", null), Instant.now());
        when(snapshotRepository.findLatestByCardPrintingIds(List.of(7L)))
                .thenReturn(List.of(snapshot));

        var prices = cardPriceService.latestPrices(List.of(7L));

        assertThat(prices).containsOnlyKeys(7L);
        assertThat(prices.get(7L).usd()).isEqualByComparingTo(new BigDecimal("3.25"));
        assertThat(prices.get(7L).eur()).isEqualByComparingTo(new BigDecimal("2.90"));
    }

    @Test
    void shouldReturnEmptyPricesWhenNoPrintingsGiven() {
        assertThat(cardPriceService.latestPrices(List.of())).isEmpty();
        verify(snapshotRepository, never()).findLatestByCardPrintingIds(any());
    }

    private static CardPrinting printing(long id, String scryfallCardId) {
        var printing = new CardPrinting(null, null, scryfallCardId);
        ReflectionTestUtils.setField(printing, "id", id);
        return printing;
    }

    private static CardPrice price(String usd, String usdFoil, String eur, String tix) {
        return new CardPrice(decimal(usd), decimal(usdFoil), decimal(eur), decimal(tix));
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
