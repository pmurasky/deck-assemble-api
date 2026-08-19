package com.deckassemble.decks.api;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardExportView;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.application.exporting.DeckExportFormat;
import com.deckassemble.decks.application.exporting.DeckExporter;
import com.deckassemble.decks.domain.DeckCard;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/decks")
class DeckExportController {

    private final DeckService deckService;
    private final DeckCardService deckCardService;
    private final CardCatalogService cardCatalogService;
    private final Map<DeckExportFormat, DeckExporter> exporters;

    DeckExportController(
            DeckService deckService,
            DeckCardService deckCardService,
            CardCatalogService cardCatalogService,
            List<DeckExporter> exporters) {
        this.deckService = deckService;
        this.deckCardService = deckCardService;
        this.cardCatalogService = cardCatalogService;
        this.exporters =
                exporters.stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        DeckExporter::format, exporter -> exporter));
    }

    @GetMapping("/{deckId}/exports")
    ResponseEntity<byte[]> export(
            @PathVariable long deckId, @RequestParam DeckExportFormat format) {
        DeckResponse deck = deckService.getById(deckId);
        List<DeckCardResponse> cards = deckCardService.listCards(deckId);
        Map<Long, CardExportView> views =
                cardCatalogService.getExportViewsByPrintingIds(
                        cards.stream().map(DeckCardResponse::cardPrintingId).toList());
        String content =
                Objects.requireNonNull(exporters.get(format), "Missing deck exporter")
                        .export(cards.stream().map(card -> exportCard(card, views)).toList());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(format.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(deck.name(), format))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{deckId}/exports/proxy-sheet")
    ProxySheetResponse proxySheet(@PathVariable long deckId) {
        List<DeckCardResponse> cards = deckCardService.listCards(deckId);
        // ponytail: proxy sheet covers the playable deck only (commander + main), matching analysis
        List<DeckCardResponse> unowned =
                cards.stream()
                        .filter(DeckExportController::playableSection)
                        .filter(
                                card ->
                                        !DeckCard.OwnershipStatus.OWNED
                                                .name()
                                                .equals(card.ownershipStatus()))
                        .toList();
        Map<Long, CardExportView> views =
                cardCatalogService.getExportViewsByPrintingIds(
                        unowned.stream().map(DeckCardResponse::cardPrintingId).toList());
        return new ProxySheetResponse(
                unowned.stream().map(card -> toProxyCard(card, views)).toList());
    }

    private static ProxySheetResponse.ProxySheetCard toProxyCard(
            DeckCardResponse card, Map<Long, CardExportView> views) {
        CardExportView view = views.get(card.cardPrintingId());
        if (view == null) {
            throw new IllegalStateException("Deck references a missing card printing");
        }
        return new ProxySheetResponse.ProxySheetCard(
                view.displayName(), view.imageUri(), card.quantity());
    }

    private static boolean playableSection(DeckCardResponse card) {
        DeckCard.Section section = DeckCard.Section.valueOf(card.deckSection());
        return section == DeckCard.Section.COMMANDER || section == DeckCard.Section.MAIN_DECK;
    }

    private static DeckExporter.ExportCard exportCard(
            DeckCardResponse card, Map<Long, CardExportView> views) {
        CardExportView view = views.get(card.cardPrintingId());
        if (view == null) {
            throw new IllegalStateException("Deck references a missing card printing");
        }
        var printing = view.printing();
        return new DeckExporter.ExportCard(
                DeckCard.Section.valueOf(card.deckSection()),
                card.quantity(),
                view.displayName(),
                new DeckExporter.PrintingReference(
                        printing.setCode(), printing.collectorNumber(), printing.scryfallId()));
    }

    private static String attachment(String deckName, DeckExportFormat format) {
        String safeName = deckName.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        String stem = safeName.isBlank() ? "deck" : safeName;
        return ContentDisposition.attachment()
                .filename(stem + "-" + format.filenameSuffix())
                .build()
                .toString();
    }
}
