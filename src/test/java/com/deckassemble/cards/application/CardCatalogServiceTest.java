package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.CommanderPairingRules;
import com.deckassemble.cards.domain.MagicSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CardCatalogServiceTest {

    private static final MagicSet SET = new MagicSet("set-id", "tst", "Test Set");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock private CardRepository cardRepository;
    @Mock private CardPrintingRepository cardPrintingRepository;

    @Test
    void shouldFilterSearchResultsByPairingWithPrimaryCommander() {
        Card primary = card("Tymna the Weaver");
        primary.setTypeLine("Legendary Creature — Human Cleric");
        primary.setOracleText("Lifelink\nPartner");
        Card partner = card("Kraum, Ludevic's Opus");
        partner.setTypeLine("Legendary Creature — Zombie Horror");
        partner.setOracleText("Flying, haste\nPartner");
        Card nonPartner = card("Atraxa, Praetors' Voice");
        nonPartner.setTypeLine("Legendary Creature — Angel Horror");
        nonPartner.setOracleText("Flying, vigilance");
        when(cardRepository.findById(1L)).thenReturn(Optional.of(primary));
        when(cardRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(partner, nonPartner));

        Page<CardSummaryResponse> result =
                service().search("", null, null, null, null, 1L, PAGEABLE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(CardSummaryResponse::name)
                .containsExactly("Kraum, Ludevic's Opus");
    }

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

        Page<CardSummaryResponse> result =
                service().search("bolt", null, null, null, null, null, PAGEABLE);

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

    @Test
    void shouldClearStaleRanksAndAssignNewOnes() {
        Card atraxa = card("Atraxa, Praetors' Voice");
        when(cardRepository.findByNameIn(java.util.Set.of("Atraxa, Praetors' Voice")))
                .thenReturn(List.of(atraxa));

        int updated = service().updateCommanderRanks(Map.of("Atraxa, Praetors' Voice", 1));

        assertThat(updated).isEqualTo(1);
        assertThat(atraxa.getCommanderRank()).isEqualTo(1);
        verify(cardRepository).clearCommanderRanks();
    }

    @Test
    void shouldClearAllRanksWhenNewListIsEmpty() {
        when(cardRepository.findByNameIn(java.util.Set.of())).thenReturn(List.of());

        int updated = service().updateCommanderRanks(Map.of());

        assertThat(updated).isZero();
        verify(cardRepository).clearCommanderRanks();
    }

    @Test
    void shouldClearStaleGameChangersAndAssignCurrentOnes() {
        Card manaVault = card("Mana Vault");
        when(cardRepository.findByScryfallOracleIdIn(java.util.Set.of("oracle-Mana Vault")))
                .thenReturn(List.of(manaVault));

        int updated = service().updateGameChangers(java.util.Set.of("oracle-Mana Vault"));

        assertThat(updated).isEqualTo(1);
        assertThat(manaVault.getGameChanger()).isTrue();
        verify(cardRepository).clearGameChangers();
    }

    private CardCatalogService service() {
        return new CardCatalogService(
                cardRepository, cardPrintingRepository, new CommanderPairingRules());
    }

    private Card card(String name) {
        Card card = new Card("oracle-" + name, name);
        org.springframework.test.util.ReflectionTestUtils.setField(card, "id", 1L);
        return card;
    }

    @Test
    void shouldReturnActiveCardById() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card("Atraxa")));

        assertThat(service().getCard(1L).getName()).isEqualTo("Atraxa");
    }

    @Test
    void shouldThrowWhenGetCardMissing() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getCard(1L)).isInstanceOf(CardNotFoundException.class);
    }

    @Test
    void shouldMapPrintingsToCards() {
        Card card = card("Sol Ring");
        var printing = new CardPrinting(card, SET, "sc-1");
        org.springframework.test.util.ReflectionTestUtils.setField(printing, "id", 9L);
        when(cardPrintingRepository.findAllById(List.of(9L))).thenReturn(List.of(printing));

        var cards = service().getCardsByPrintingIds(List.of(9L));

        assertThat(cards.get(9L).getName()).isEqualTo("Sol Ring");
        assertThat(service().getCardsByPrintingIds(List.of())).isEmpty();
    }

    @Test
    void shouldMapCardsToLatestPrintingIds() {
        Card card = card("Forest");
        var printing = new CardPrinting(card, SET, "sc-2");
        org.springframework.test.util.ReflectionTestUtils.setField(printing, "id", 5L);
        when(cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(1L))
                .thenReturn(List.of(printing));

        var printingIds = service().getLatestPrintingIdByCardIds(List.of(1L, 2L));

        assertThat(printingIds).containsExactly(Map.entry(1L, 5L));
    }
}
