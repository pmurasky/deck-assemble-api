package com.deckassemble.imports.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardImportData;
import com.deckassemble.cards.domain.CardImportFace;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingFace;
import com.deckassemble.cards.domain.CardPrintingFaceRepository;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
class CardPrintingImporter {

    private final MagicSetRepository magicSetRepository;
    private final CardPrintingRepository cardPrintingRepository;
    private final CardPrintingFaceRepository cardPrintingFaceRepository;

    CardPrintingImporter(
            MagicSetRepository magicSetRepository,
            CardPrintingRepository cardPrintingRepository,
            CardPrintingFaceRepository cardPrintingFaceRepository) {
        this.magicSetRepository = magicSetRepository;
        this.cardPrintingRepository = cardPrintingRepository;
        this.cardPrintingFaceRepository = cardPrintingFaceRepository;
    }

    boolean importPrinting(Card card, CardImportData source) {
        var existing = cardPrintingRepository.findByScryfallCardId(source.id());
        MagicSet set =
                magicSetRepository
                        .findBySetCode(source.set())
                        .orElseGet(
                                () ->
                                        magicSetRepository.save(
                                                new MagicSet(
                                                        source.setId(),
                                                        source.set(),
                                                        source.setName())));
        CardPrinting printing = existing.orElseGet(() -> new CardPrinting(card, set, source.id()));
        applyDetails(printing, source);
        printing = cardPrintingRepository.save(printing);
        replaceFaces(printing, source);
        return existing.isPresent();
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private void applyDetails(CardPrinting printing, CardImportData source) {
        printing.setCollectorNumber(source.collectorNumber());
        printing.setRarity(source.rarity());
        printing.setArtist(source.artist());
        printing.setFlavorText(source.flavorText());
        printing.setFlavorName(source.flavorName());
        printing.setReleasedAt(source.releasedAt());
        printing.setFoilAvailable(source.foil());
        printing.setNonfoilAvailable(source.nonfoil());
        printing.setPromo(source.promo());
        printing.setDigital(source.digital());
        printing.setLanguage(source.lang());
        if (source.images() != null) {
            printing.setImageUriSmall(source.images().small());
            printing.setImageUriNormal(source.images().normal());
            printing.setImageUriLarge(source.images().large());
        }
    }

    private void replaceFaces(CardPrinting printing, CardImportData source) {
        cardPrintingFaceRepository.deleteByCardPrintingId(printing.getId());
        var faces = new ArrayList<CardPrintingFace>();
        for (int order = 0; order < source.faces().size(); order++) {
            CardImportFace face = source.faces().get(order);
            if (face.imageUri() != null) {
                faces.add(new CardPrintingFace(printing, order, face.name(), face.imageUri()));
            }
        }
        cardPrintingFaceRepository.saveAll(faces);
    }
}
