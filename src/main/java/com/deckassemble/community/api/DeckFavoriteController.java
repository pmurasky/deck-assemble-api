package com.deckassemble.community.api;

import com.deckassemble.community.application.FavoriteService;
import com.deckassemble.community.domain.DeckFavorite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeckFavoriteController {

    private final FavoriteService favoriteService;

    public DeckFavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/shared/decks/{slug}/favorite")
    public ResponseEntity<DeckFavorite> favorite(@PathVariable String slug) {
        FavoriteService.FavoriteResult result = favoriteService.favorite(slug);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.favorite());
    }

    @DeleteMapping("/shared/decks/{slug}/favorite")
    public ResponseEntity<Void> unfavorite(@PathVariable String slug) {
        favoriteService.unfavorite(slug);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/community/favorites")
    public DeckDiscoveryResponse favorites(@PageableDefault(size = 20) Pageable pageable) {
        return DeckDiscoveryResponse.from(favoriteService.listFavorites(pageable));
    }
}
