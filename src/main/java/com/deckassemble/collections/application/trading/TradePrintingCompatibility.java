package com.deckassemble.collections.application.trading;

import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class TradePrintingCompatibility {

    private final CardPrintingRepository printingRepository;

    TradePrintingCompatibility(CardPrintingRepository printingRepository) {
        this.printingRepository = printingRepository;
    }

    boolean compatible(long offeredPrintingId, long wantedPrintingId) {
        Map<Long, CardPrinting> printings = printings(offeredPrintingId, wantedPrintingId);
        CardPrinting offered = printings.get(offeredPrintingId);
        CardPrinting wanted = printings.get(wantedPrintingId);
        return offered != null
                && wanted != null
                && offered.getCard()
                        .getScryfallOracleId()
                        .equals(wanted.getCard().getScryfallOracleId());
    }

    private Map<Long, CardPrinting> printings(long offeredPrintingId, long wantedPrintingId) {
        return printingRepository.findAllById(List.of(offeredPrintingId, wantedPrintingId)).stream()
                .collect(Collectors.toMap(CardPrinting::getId, printing -> printing));
    }
}
