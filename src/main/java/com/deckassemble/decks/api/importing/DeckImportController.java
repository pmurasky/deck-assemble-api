package com.deckassemble.decks.api.importing;

import com.deckassemble.decks.application.importing.DeckImportService;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
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
}
