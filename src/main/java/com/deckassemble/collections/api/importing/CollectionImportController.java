package com.deckassemble.collections.api.importing;

import com.deckassemble.collections.application.importing.CollectionImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/collections/imports")
public class CollectionImportController {

    private final CollectionImportService importService;

    public CollectionImportController(CollectionImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CollectionImportPreviewResponse preview(
            @RequestParam CollectionImportPreset preset,
            CollectionColumnMapping overrides,
            @RequestPart("file") MultipartFile file)
            throws IOException {
        var layout = preset.defaultMapping().merge(overrides).toLayout();
        return new CollectionImportPreviewResponse(importService.preview(layout, file.getBytes()));
    }

    @PostMapping
    public ResponseEntity<CollectionImportResultResponse> commit(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 255) String idempotencyKey,
            @Valid @RequestBody CommitCollectionImportRequest request) {
        var result =
                importService.commit(
                        request.previewToken(),
                        request.name(),
                        request.excludedLineNumbers(),
                        idempotencyKey);
        return ResponseEntity.created(URI.create("/api/v1/collections/" + result.collection().id()))
                .body(new CollectionImportResultResponse(result));
    }

    @GetMapping("/{token}/errors")
    public ResponseEntity<byte[]> errors(@PathVariable UUID token) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"collection-import-errors.csv\"")
                .body(importService.errors(token));
    }
}
