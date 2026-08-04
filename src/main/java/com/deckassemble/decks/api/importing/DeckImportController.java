package com.deckassemble.decks.api.importing;

import com.deckassemble.decks.application.importing.DeckImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/decks/imports")
public class DeckImportController {

    private final DeckImportService importService;

    public DeckImportController(DeckImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DeckImportPreviewResponse preview(
            @RequestParam DeckImportFormat format, @RequestPart("file") MultipartFile file)
            throws IOException {
        return new DeckImportPreviewResponse(importService.preview(format.name(), file.getBytes()));
    }

    @PostMapping
    public ResponseEntity<DeckImportResultResponse> commit(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 255) String idempotencyKey,
            @Valid @RequestBody CommitDeckImportRequest request) {
        var result =
                importService.commit(
                        request.previewToken(),
                        request.name(),
                        request.excludedLineNumbers(),
                        idempotencyKey);
        return ResponseEntity.created(URI.create("/api/v1/decks/" + result.deck().id()))
                .body(new DeckImportResultResponse(result));
    }
}
