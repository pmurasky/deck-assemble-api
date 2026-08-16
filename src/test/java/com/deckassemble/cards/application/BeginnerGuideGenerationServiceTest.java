package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideContent;
import com.deckassemble.cards.domain.BeginnerGuideGenerator;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.BeginnerGuideSource;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.ScryfallClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BeginnerGuideGenerationServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private CardPrintingRepository cardPrintingRepository;
    @Mock private ScryfallClient scryfallClient;
    @Mock private BeginnerGuideGenerator generator;
    @Mock private BeginnerGuideRepository guideRepository;
    @InjectMocks private BeginnerGuideGenerationService service;

    @Test
    void shouldGenerateDraftFromEveryFaceAndLatestPrintingRulings() {
        var card = multifaceCard();
        var printing = new CardPrinting(card, null, "latest-printing");
        when(cardRepository.findById(42L)).thenReturn(Optional.of(card));
        when(cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(42L))
                .thenReturn(List.of(printing));
        when(scryfallClient.getRulings("latest-printing")).thenReturn(List.of("Ruling one"));
        when(generator.generate(
                        new BeginnerGuideSource(
                                "Spider-Man",
                                List.of("Front text", "Back text"),
                                List.of("Ruling one"))))
                .thenReturn(new BeginnerGuideContent("Summary", "Examples", "When to use"));

        service.generate(42L);

        var guide = ArgumentCaptor.forClass(BeginnerGuide.class);
        verify(guideRepository).save(guide.capture());
        assertThat(guide.getValue().getSummary()).isEqualTo("Summary");
        assertThat(guide.getValue().getSourceRulingsSnapshot()).isEqualTo("Ruling one");
        assertThat(guide.getValue().getSourceOracleHash()).hasSize(64);
    }

    @Test
    void shouldReturnExistingGuideWithoutRegeneration() {
        var existing = org.mockito.Mockito.mock(BeginnerGuide.class);
        when(guideRepository.findById(42L)).thenReturn(Optional.of(existing));

        var result = service.generate(42L);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(cardRepository, cardPrintingRepository, scryfallClient, generator);
    }

    @Test
    void shouldRetainGenerationRequester() {
        var card = multifaceCard();
        when(cardRepository.findById(42L)).thenReturn(Optional.of(card));
        when(cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(42L))
                .thenReturn(List.of(new CardPrinting(card, null, "printing-id")));
        when(scryfallClient.getRulings("printing-id")).thenReturn(List.of());
        when(generator.generate(
                        new BeginnerGuideSource(
                                "Spider-Man", List.of("Front text", "Back text"), List.of())))
                .thenReturn(new BeginnerGuideContent("Summary", "Examples", "When to use"));

        service.generate(42L, "user-1");

        var guide = ArgumentCaptor.forClass(BeginnerGuide.class);
        verify(guideRepository).save(guide.capture());
        assertThat(guide.getValue().getGeneratedBy()).isEqualTo("user-1");
    }

    @Test
    void shouldReplaceExistingGuideDuringRegeneration() {
        var card = multifaceCard();
        when(guideRepository.findById(42L)).thenReturn(Optional.empty());
        when(cardRepository.findById(42L)).thenReturn(Optional.of(card));
        when(cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(42L))
                .thenReturn(List.of(new CardPrinting(card, null, "printing-id")));
        when(scryfallClient.getRulings("printing-id")).thenReturn(List.of());
        when(generator.generate(
                        new BeginnerGuideSource(
                                "Spider-Man", List.of("Front text", "Back text"), List.of())))
                .thenReturn(new BeginnerGuideContent("Fresh", "Examples", "When to use"));
        when(guideRepository.save(any(BeginnerGuide.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BeginnerGuide result = service.regenerate(42L);

        verify(guideRepository).deleteById(42L);
        verify(guideRepository).flush();
        assertThat(result.getSummary()).isEqualTo("Fresh");
        assertThat(result.getGeneratedBy()).isNull();
    }

    private static Card multifaceCard() {
        var card = new Card("oracle-id", "Spider-Man");
        ReflectionTestUtils.setField(card, "id", 42L);
        card.getFaces().add(face(card, 0, "Front", "Front text"));
        card.getFaces().add(face(card, 1, "Back", "Back text"));
        return card;
    }

    private static CardFace face(Card card, int order, String name, String oracleText) {
        var face = new CardFace(card, order, name);
        face.setOracleText(oracleText);
        return face;
    }
}
