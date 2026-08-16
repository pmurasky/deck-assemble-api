package com.deckassemble.cards.infrastructure.config;

import com.deckassemble.cards.application.BeginnerGuideGenerationQuota;
import com.deckassemble.cards.application.BeginnerGuideGenerationService;
import com.deckassemble.cards.application.BeginnerGuideRequestService;
import com.deckassemble.cards.domain.BeginnerGuideGenerator;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.ScryfallClient;
import com.deckassemble.shared.security.CurrentUser;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BeginnerGuideConfiguration {

    @Bean
    BeginnerGuideGenerationService beginnerGuideGenerationService(
            CardRepository cardRepository,
            CardPrintingRepository printingRepository,
            ScryfallClient scryfallClient,
            BeginnerGuideGenerator generator,
            BeginnerGuideRepository guideRepository) {
        return new BeginnerGuideGenerationService(
                cardRepository, printingRepository, scryfallClient, generator, guideRepository);
    }

    @Bean
    BeginnerGuideGenerationQuota beginnerGuideGenerationQuota(
            CurrentUser currentUser,
            BeginnerGuideRepository guideRepository,
            @Value("${deckassemble.beginner-guide.daily-limit:5}") int dailyLimit) {
        return new BeginnerGuideGenerationQuota(
                currentUser, guideRepository, Clock.systemUTC(), dailyLimit);
    }

    @Bean
    BeginnerGuideRequestService beginnerGuideRequestService(
            BeginnerGuideRepository guideRepository,
            BeginnerGuideGenerationQuota quota,
            BeginnerGuideGenerationService generationService) {
        return new BeginnerGuideRequestService(guideRepository, quota, generationService);
    }
}
