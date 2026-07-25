package com.deckassemble;

import com.deckassemble.cards.infrastructure.scryfall.ScryfallProperties;
import com.deckassemble.recommendations.infrastructure.edhrec.EdhrecProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

// checkstyle:HideUtilityClassConstructor suppressed: Spring Boot requires a non-final class
// with an implicit public constructor for the application entry point.
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties({ScryfallProperties.class, EdhrecProperties.class})
public class DeckAssembleApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeckAssembleApplication.class, args);
    }
}
