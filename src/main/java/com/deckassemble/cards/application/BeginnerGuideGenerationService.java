package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideContent;
import com.deckassemble.cards.domain.BeginnerGuideDraft;
import com.deckassemble.cards.domain.BeginnerGuideGenerator;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.BeginnerGuideSource;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.ScryfallClient;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class BeginnerGuideGenerationService {
    private final CardRepository cardRepository;
    private final CardPrintingRepository printingRepository;
    private final ScryfallClient scryfallClient;
    private final BeginnerGuideGenerator generator;
    private final BeginnerGuideRepository guideRepository;

    public BeginnerGuideGenerationService(
            CardRepository cardRepository,
            CardPrintingRepository printingRepository,
            ScryfallClient scryfallClient,
            BeginnerGuideGenerator generator,
            BeginnerGuideRepository guideRepository) {
        this.cardRepository = cardRepository;
        this.printingRepository = printingRepository;
        this.scryfallClient = scryfallClient;
        this.generator = generator;
        this.guideRepository = guideRepository;
    }

    @Transactional
    public BeginnerGuide generate(Long cardId) {
        var card = cardRepository.findById(cardId).orElseThrow(CardNotFoundException::new);
        var printing =
                printingRepository.findByCardIdOrderByReleasedAtDesc(cardId).stream()
                        .findFirst()
                        .orElseThrow(CardNotFoundException::new);
        var source = source(card, scryfallClient.getRulings(printing.getScryfallCardId()));
        var content = generator.generate(source);
        return guideRepository.save(toGuide(cardId, source, content));
    }

    private static BeginnerGuideSource source(Card card, List<String> rulings) {
        var oracleTexts =
                card.getFaces().isEmpty()
                        ? List.of(card.getOracleText())
                        : card.getFaces().stream().map(face -> face.getOracleText()).toList();
        return new BeginnerGuideSource(card.getName(), oracleTexts, rulings);
    }

    private static BeginnerGuide toGuide(
            Long cardId, BeginnerGuideSource source, BeginnerGuideContent content) {
        var draft =
                new BeginnerGuideDraft(
                        content.summary(),
                        content.examples(),
                        content.whenToUse(),
                        String.join("\n", source.rulings()),
                        source.oracleHash());
        return new BeginnerGuide(cardId, draft, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
