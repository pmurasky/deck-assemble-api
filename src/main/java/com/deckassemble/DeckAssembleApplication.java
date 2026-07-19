package com.deckassemble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DeckAssembleApplication {

  public static void main(String[] args) {
    SpringApplication.run(DeckAssembleApplication.class, args);
  }
}
