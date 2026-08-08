package com.deckassemble.shared.exception;

import com.deckassemble.cards.application.CardNotFoundException;
import com.deckassemble.cards.application.FinishUnavailableException;
import com.deckassemble.cards.application.InvalidCardSearchFilterException;
import com.deckassemble.collections.application.CollectionCardNotFoundException;
import com.deckassemble.collections.application.CollectionNotFoundException;
import com.deckassemble.decks.application.DeckCardNotFoundException;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.application.organization.CategoryTemplateNotFoundException;
import com.deckassemble.decks.application.organization.DeckCategoryNotFoundException;
import com.deckassemble.decks.application.organization.DeckFolderNotFoundException;
import com.deckassemble.decks.application.organization.DeckTagNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CardNotFoundException.class)
    ProblemDetail handleCardNotFound() {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Card not found.");
        problem.setType(URI.create("https://deckassemble.app/problems/card-not-found"));
        problem.setTitle("Card not found");
        problem.setProperty("code", "CARD_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(CollectionNotFoundException.class)
    ProblemDetail handleCollectionNotFound() {
        return notFound("collection", "Collection");
    }

    @ExceptionHandler(CollectionCardNotFoundException.class)
    ProblemDetail handleCollectionCardNotFound() {
        return notFound("collection-card", "Collection card");
    }

    @ExceptionHandler(DeckNotFoundException.class)
    ProblemDetail handleDeckNotFound() {
        return notFound("deck", "Deck");
    }

    @ExceptionHandler(DeckCardNotFoundException.class)
    ProblemDetail handleDeckCardNotFound() {
        return notFound("deck-card", "Deck card");
    }

    @ExceptionHandler(DeckCategoryNotFoundException.class)
    ProblemDetail handleDeckCategoryNotFound() {
        return notFound("deck-category", "Deck category");
    }

    @ExceptionHandler(DeckFolderNotFoundException.class)
    ProblemDetail handleDeckFolderNotFound() {
        return notFound("deck-folder", "Deck folder");
    }

    @ExceptionHandler(DeckTagNotFoundException.class)
    ProblemDetail handleDeckTagNotFound() {
        return notFound("deck-tag", "Deck tag");
    }

    @ExceptionHandler(CategoryTemplateNotFoundException.class)
    ProblemDetail handleCategoryTemplateNotFound() {
        return notFound("category-template", "Category template");
    }

    @ExceptionHandler(InvalidCardSearchFilterException.class)
    ProblemDetail handleInvalidCardSearchFilter(InvalidCardSearchFilterException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("https://deckassemble.app/problems/invalid-search-filter"));
        problem.setTitle("Invalid search filter");
        problem.setProperty("code", "INVALID_SEARCH_FILTER");
        return problem;
    }

    @ExceptionHandler(FinishUnavailableException.class)
    ProblemDetail handleFinishUnavailable(FinishUnavailableException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setType(URI.create("https://deckassemble.app/problems/finish-unavailable"));
        problem.setTitle("Finish unavailable");
        problem.setProperty("code", "FINISH_UNAVAILABLE");
        return problem;
    }

    private ProblemDetail notFound(String resource, String title) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, title + " not found.");
        problem.setType(URI.create("https://deckassemble.app/problems/" + resource + "-not-found"));
        problem.setTitle(title + " not found");
        problem.setProperty("code", resource.toUpperCase().replace('-', '_') + "_NOT_FOUND");
        return problem;
    }
}
