package com.deckassemble.decks.api.organization;

import com.deckassemble.decks.application.organization.CategoryTemplateService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CRUD for a profile's reusable category templates. */
@RestController
@RequestMapping("/category-templates")
public class CategoryTemplateController {

    private final CategoryTemplateService categoryTemplateService;

    public CategoryTemplateController(CategoryTemplateService categoryTemplateService) {
        this.categoryTemplateService = categoryTemplateService;
    }

    @GetMapping
    public List<CategoryTemplateResponse> list() {
        return categoryTemplateService.list().stream().map(CategoryTemplateResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<CategoryTemplateResponse> create(
            @Valid @RequestBody CategoryTemplateRequest request) {
        CategoryTemplateResponse created =
                CategoryTemplateResponse.from(
                        categoryTemplateService.create(request.name(), request.itemNames()));
        return ResponseEntity.created(URI.create("/api/v1/category-templates/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{templateId}")
    public CategoryTemplateResponse update(
            @PathVariable long templateId, @Valid @RequestBody CategoryTemplateRequest request) {
        return CategoryTemplateResponse.from(
                categoryTemplateService.update(templateId, request.name(), request.itemNames()));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> delete(@PathVariable long templateId) {
        categoryTemplateService.delete(templateId);
        return ResponseEntity.noContent().build();
    }
}
