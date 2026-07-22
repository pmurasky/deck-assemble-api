package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CardCatalogServiceTest {

    private static final MagicSet SET = new MagicSet("set-id", "tst", "Test Set");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock private CardRepository cardRepository;
    @Mock private CardPrintingRepository cardPrintingRepository;

    @Test
    void shouldSearchWithLatestPrintingMapped() {
        Card card = card("Lightning Bolt");
        CardPrinting printing = new CardPrinting(card, SET, "scry-1");
        printing.setFoilAvailable(true);
        printing.setNonfoilAvailable(false);
        when(cardRepository.findAll(any(Specification.class), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(card)));
        when(cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(anyLong()))
                .thenReturn(List.of(printing));

        Page<CardSummaryResponse> result = service().search("bolt", null, null, null, PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
        CardSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.foilAvailable()).isTrue();
        assertThat(summary.nonfoilAvailable()).isFalse();
    }

    @Test
    void shouldReturnDetailForActiveCard() {
        Card card = card("Lightning Bolt");
        CardPrinting printing = new CardPrinting(card, SET, "scry-1");
        printing.setFoilAvailable(true);
        printing.setNonfoilAvailable(true);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(anyLong()))
                .thenReturn(List.of(printing));

        CardDetailResponse detail = service().getById(1L);
        assertThat(detail.name()).isEqualTo("Lightning Bolt");
        assertThat(detail.foilAvailable()).isTrue();
        assertThat(detail.nonfoilAvailable()).isTrue();
    }

    @Test
    void shouldThrowWhenCardMissing() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getById(1L)).isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void shouldThrowWhenCardInactive() {
        Card card = card("Old Card");
        card.setActive(false);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service().getById(1L)).isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void shouldReturnSummaryByPrintingId() {
        Card card = card("Lightning Bolt");
        CardPrinting printing = new CardPrinting(card, SET, "scry-1");
        printing.setFoilAvailable(false);
        printing.setNonfoilAvailable(true);
        when(cardPrintingRepository.findById(10L)).thenReturn(Optional.of(printing));

        CardSummaryResponse summary = service().getSummaryByPrintingId(10L);
        assertThat(summary.name()).isEqualTo("Lightning Bolt");
        assertThat(summary.foilAvailable()).isFalse();
        assertThat(summary.nonfoilAvailable()).isTrue();
    }

    @Test
    void shouldThrowWhenPrintingInactive() {
        CardPrinting printing = new CardPrinting(card("Bolt"), SET, "scry-1");
        printing.setActive(false);
        when(cardPrintingRepository.findById(10L)).thenReturn(Optional.of(printing));

        assertThatThrownBy(() -> service().getSummaryByPrintingId(10L))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void shouldReturnNameById() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card("Lightning Bolt")));

        assertThat(service().getNameById(1L)).isEqualTo("Lightning Bolt");
    }

    @Test
    void shouldReturnNullNameWhenCardMissing() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(service().getNameById(1L)).isNull();
    }

    @Test
    void shouldListSetPrintingsWithoutQuery() {
        CardPrinting printing = new CardPrinting(card("Bolt"), SET, "scry-1");
        when(cardPrintingRepository.findByMagicSetSetCodeAndActiveTrueAndCardActiveTrue(
                        "tst", PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(printing)));

        Page<CardSummaryResponse> result = service().getSetPrintings("tst", PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldListSetPrintingsWithQuery() {
        CardPrinting printing = new CardPrinting(card("Bolt"), SET, "scry-1");
        when(cardPrintingRepository
                        .findByMagicSetSetCodeAndActiveTrueAndCardActiveTrueAndCardNameContainingIgnoreCase(
                                "tst", "bolt", PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(printing)));

        Page<CardSummaryResponse> result = service().getSetPrintings("tst", "bolt", PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldListPrintingsForCard() {
        Card card = card("Bolt");
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        CardPrinting printing = new CardPrinting(card, SET, "scry-1");
        printing.setFoilAvailable(true);
        printing.setNonfoilAvailable(false);
        when(cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(1L))
                .thenReturn(List.of(printing));

        List<CardPrintingResponse> result = service().getPrintings(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).foilAvailable()).isTrue();
        assertThat(result.get(0).nonfoilAvailable()).isFalse();
    }

    @Test
    void shouldRejectFoilQuantityWhenFoilUnavailable() {
        CardPrinting printing = new CardPrinting(card("Bolt"), SET, "scry-1");
        printing.setFoilAvailable(false);
        printing.setNonfoilAvailable(true);
        when(cardPrintingRepository.findById(10L)).thenReturn(Optional.of(printing));

        assertThatThrownBy(() -> service().validateFinishAvailability(10L, 1, 1))
                .isInstanceOf(FinishUnavailableException.class)
                .hasMessageContaining("foil");
    }

    @Test
    void shouldRejectRegularQuantityWhenNonfoilUnavailable() {
        CardPrinting printing = new CardPrinting(card("Bolt"), SET, "scry-1");
        printing.setFoilAvailable(true);
        printing.setNonfoilAvailable(false);
        when(cardPrintingRepository.findById(10L)).thenReturn(Optional.of(printing));

        assertThatThrownBy(() -> service().validateFinishAvailability(10L, 1, 0))
                .isInstanceOf(FinishUnavailableException.class)
                .hasMessageContaining("nonfoil");
    }

    @Test
    void shouldAcceptQuantitiesWhenFinishesAvailable() {
        CardPrinting printing = new CardPrinting(card("Bolt"), SET, "scry-1");
        printing.setFoilAvailable(true);
        printing.setNonfoilAvailable(true);
        when(cardPrintingRepository.findById(10L)).thenReturn(Optional.of(printing));

        service().validateFinishAvailability(10L, 2, 3);
    }

    @Test
    void shouldThrowWhenValidatingMissingPrinting() {
        when(cardPrintingRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().validateFinishAvailability(10L, 1, 0))
                .isInstanceOf(CardNotFoundException.class);
    }

    private CardCatalogService service() {
        return new CardCatalogService(cardRepository, cardPrintingRepository);
    }

    private Card card(String name) {
        Card card = new Card("oracle-" + name, name);
        org.springframework.test.util.ReflectionTestUtils.setField(card, "id", 1L);
        return card;
    }
}
