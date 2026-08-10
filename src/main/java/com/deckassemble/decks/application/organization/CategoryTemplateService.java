package com.deckassemble.decks.application.organization;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.domain.organization.CategoryTemplate;
import com.deckassemble.decks.domain.organization.CategoryTemplateItem;
import com.deckassemble.decks.domain.organization.CategoryTemplateItemRepository;
import com.deckassemble.decks.domain.organization.CategoryTemplateRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manages a profile's reusable, ordered category-name templates and their application to a deck.
 * Applying a template creates a {@code DeckCategory} (via {@link DeckCategoryService}) for every
 * item whose name isn't already present on the deck (case-insensitively), so applying the same
 * template twice never creates duplicates.
 */
@Service
@Transactional
public class CategoryTemplateService {

    private final DeckAccessGuard deckAccessGuard;
    private final CategoryTemplateRepository templateRepository;
    private final CategoryTemplateItemRepository itemRepository;
    private final DeckCategoryService deckCategoryService;

    public CategoryTemplateService(
            DeckAccessGuard deckAccessGuard,
            CategoryTemplateRepository templateRepository,
            CategoryTemplateItemRepository itemRepository,
            DeckCategoryService deckCategoryService) {
        this.deckAccessGuard = deckAccessGuard;
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
        this.deckCategoryService = deckCategoryService;
    }

    public List<TemplateView> list() {
        return templateRepository
                .findByProfileIdOrderByNameAsc(deckAccessGuard.profileId())
                .stream()
                .map(this::viewOf)
                .toList();
    }

    public TemplateView create(String name, List<String> itemNames) {
        long profileId = deckAccessGuard.profileId();
        assertNameAvailable(profileId, name);
        CategoryTemplate saved = templateRepository.save(new CategoryTemplate(profileId, name));
        saveItems(saved.getId(), itemNames);
        return viewOf(saved);
    }

    public TemplateView update(long templateId, String name, List<String> itemNames) {
        long profileId = deckAccessGuard.profileId();
        CategoryTemplate template = ownedTemplate(profileId, templateId);
        if (!template.getName().equals(name)) {
            assertNameAvailable(profileId, name);
            template.setName(name);
        }
        templateRepository.save(template);
        itemRepository.deleteByCategoryTemplateId(templateId);
        itemRepository.flush();
        saveItems(templateId, itemNames);
        return viewOf(template);
    }

    public void delete(long templateId) {
        long profileId = deckAccessGuard.profileId();
        CategoryTemplate template = ownedTemplate(profileId, templateId);
        itemRepository.deleteByCategoryTemplateId(templateId);
        templateRepository.delete(template);
    }

    public List<DeckCategoryService.CategoryView> applyToDeck(long deckId, long templateId) {
        deckAccessGuard.owned(deckId);
        CategoryTemplate template = ownedTemplate(deckAccessGuard.profileId(), templateId);
        List<CategoryTemplateItem> items =
                itemRepository.findByCategoryTemplateIdOrderByDisplayOrderAscIdAsc(
                        template.getId());
        Set<String> existingNames =
                deckCategoryService.list(deckId).stream()
                        .map(view -> view.name().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
        for (CategoryTemplateItem item : items) {
            if (existingNames.add(item.getName().toLowerCase(Locale.ROOT))) {
                deckCategoryService.create(deckId, item.getName(), null);
            }
        }
        return deckCategoryService.list(deckId);
    }

    private void saveItems(long templateId, List<String> itemNames) {
        int order = 0;
        for (String itemName : itemNames) {
            itemRepository.save(new CategoryTemplateItem(templateId, itemName, order++));
        }
    }

    private CategoryTemplate ownedTemplate(long profileId, long templateId) {
        return templateRepository
                .findByIdAndProfileId(templateId, profileId)
                .orElseThrow(CategoryTemplateNotFoundException::new);
    }

    private void assertNameAvailable(long profileId, String name) {
        if (templateRepository.existsByProfileIdAndNameIgnoreCase(profileId, name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A template named '" + name + "' already exists");
        }
    }

    private TemplateView viewOf(CategoryTemplate template) {
        List<String> itemNames =
                itemRepository
                        .findByCategoryTemplateIdOrderByDisplayOrderAscIdAsc(template.getId())
                        .stream()
                        .map(CategoryTemplateItem::getName)
                        .toList();
        return new TemplateView(template.getId(), template.getName(), itemNames);
    }

    /** Read-only projection of a template; no JPA entities escape this service. */
    public record TemplateView(@Nullable Long id, String name, List<String> itemNames) {}
}
