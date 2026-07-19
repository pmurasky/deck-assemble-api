package com.deckassemble.shared.exception;

import com.deckassemble.cards.application.CardNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(CardNotFoundException.class)
  ProblemDetail handleCardNotFound() {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Card not found.");
    problem.setType(URI.create("https://deckassemble.app/problems/card-not-found"));
    problem.setTitle("Card not found");
    problem.setProperty("code", "CARD_NOT_FOUND");
    return problem;
  }
}
