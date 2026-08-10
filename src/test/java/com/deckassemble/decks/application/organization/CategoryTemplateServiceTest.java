package com.deckassemble.decks.application.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.organization.CategoryTemplate;
import com.deckassemble.decks.domain.organization.CategoryTemplateItem;
import com.deckassemble.decks.domain.organization.CategoryTemplateItemRepository;
import com.deckassemble.decks.domain.organization.CategoryTemplateRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CategoryTemplateServiceTest {

    private static final long PROFILE_ID = 42L;
    private static final long DECK_ID = 1L;
    private static final long TEMPLATE_ID = 200L;

    @Mock private CategoryTemplateRepository templateRepository;
    @Mock private CategoryTemplateItemRepository itemRepository;
    @Mock private DeckAccessGuard deckAccessGuard;
    @Mock private DeckCategoryService deckCategoryService;

    private final List<CategoryTemplateItem> savedItems = new ArrayList<>();
    private final AtomicLong nextItemId = new AtomicLong(1000L);

    private CategoryTemplateService service;

    @BeforeEach
    void stubCommonCollaborators() {
        lenient().when(deckAccessGuard.profileId()).thenReturn(PROFILE_ID);
        lenient()
                .when(templateRepository.save(any(CategoryTemplate.class)))
                .thenAnswer(
                        inv -> {
                            CategoryTemplate template = inv.getArgument(0);
                            if (template.getId() == null) {
                                ReflectionTestUtils.setField(template, "id", TEMPLATE_ID);
                            }
                            return template;
                        });
        lenient()
                .when(itemRepository.save(any(CategoryTemplateItem.class)))
                .thenAnswer(
                        inv -> {
                            CategoryTemplateItem item = inv.getArgument(0);
                            if (item.getId() == null) {
                                ReflectionTestUtils.setField(
                                        item, "id", nextItemId.incrementAndGet());
                            }
                            savedItems.add(item);
                            return item;
                        });
        lenient()
                .when(
                        itemRepository.findByCategoryTemplateIdOrderByDisplayOrderAscIdAsc(
                                TEMPLATE_ID))
                .thenReturn(savedItems);
        service =
                new CategoryTemplateService(
                        deckAccessGuard, templateRepository, itemRepository, deckCategoryService);
    }

    @Test
    void shouldCreateTemplateWithOrderedItems() {
        when(templateRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "Ramp Focus"))
                .thenReturn(false);

        CategoryTemplateService.TemplateView created =
                service.create("Ramp Focus", List.of("Ramp", "Removal", "Wincons"));

        assertThat(created.name()).isEqualTo("Ramp Focus");
        assertThat(created.itemNames()).containsExactly("Ramp", "Removal", "Wincons");
        assertThat(savedItems.get(0).getDisplayOrder()).isZero();
        assertThat(savedItems.get(2).getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void shouldRejectDuplicateTemplateNameCaseInsensitively() {
        when(templateRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "ramp focus"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create("ramp focus", List.of("Ramp")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldUpdateTemplateRenamingAndReplacingItems() {
        CategoryTemplate template = stubOwnedTemplate();
        when(templateRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "Control Shell"))
                .thenReturn(false);
        savedItems.add(new CategoryTemplateItem(TEMPLATE_ID, "Old Item", 0));
        // The mock repository doesn't apply deletes on its own; mirror what a real delete would
        // do to the rows findByCategoryTemplateId subsequently returns.
        doAnswer(
                        inv -> {
                            savedItems.clear();
                            return null;
                        })
                .when(itemRepository)
                .deleteByCategoryTemplateId(TEMPLATE_ID);

        CategoryTemplateService.TemplateView updated =
                service.update(TEMPLATE_ID, "Control Shell", List.of("Counterspells", "Wraths"));

        assertThat(updated.name()).isEqualTo("Control Shell");
        assertThat(template.getName()).isEqualTo("Control Shell");
        verify(itemRepository).deleteByCategoryTemplateId(TEMPLATE_ID);
        assertThat(savedItems)
                .extracting(CategoryTemplateItem::getName)
                .containsExactly("Counterspells", "Wraths");
    }

    @Test
    void shouldRejectUpdatingTemplateToDuplicateName() {
        stubOwnedTemplate();
        when(templateRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "Taken Name"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(TEMPLATE_ID, "Taken Name", List.of("Item")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldApplyTemplateCreatingOnlyMissingCategories() {
        stubOwnedTemplateForApply();
        savedItems.add(new CategoryTemplateItem(TEMPLATE_ID, "Ramp", 0));
        savedItems.add(new CategoryTemplateItem(TEMPLATE_ID, "Removal", 1));
        when(deckCategoryService.list(DECK_ID))
                .thenReturn(
                        List.of(
                                new DeckCategoryService.CategoryView(
                                        1L, "Ramp", 0, false, null, List.of(), 0)));

        service.applyToDeck(DECK_ID, TEMPLATE_ID);

        verify(deckCategoryService, never()).create(eq(DECK_ID), eq("Ramp"), any());
        verify(deckCategoryService, times(1)).create(DECK_ID, "Removal", null);
    }

    @Test
    void shouldApplyTemplateIdempotentlyWithoutDuplicates() {
        stubOwnedTemplateForApply();
        savedItems.add(new CategoryTemplateItem(TEMPLATE_ID, "Ramp", 0));
        // First application: deck has no categories yet.
        when(deckCategoryService.list(DECK_ID)).thenReturn(List.of());

        service.applyToDeck(DECK_ID, TEMPLATE_ID);

        verify(deckCategoryService, times(1)).create(DECK_ID, "Ramp", null);

        // Second application: deck now already has "Ramp" from the first apply.
        when(deckCategoryService.list(DECK_ID))
                .thenReturn(
                        List.of(
                                new DeckCategoryService.CategoryView(
                                        1L, "Ramp", 0, false, null, List.of(), 0)));

        service.applyToDeck(DECK_ID, TEMPLATE_ID);

        verify(deckCategoryService, times(1)).create(DECK_ID, "Ramp", null);
    }

    @Test
    void shouldRejectApplyingNonexistentTemplate() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(new Deck(PROFILE_ID, "Deck", "COMMANDER"));
        when(templateRepository.findByIdAndProfileId(TEMPLATE_ID, PROFILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyToDeck(DECK_ID, TEMPLATE_ID))
                .isInstanceOf(CategoryTemplateNotFoundException.class);
    }

    @Test
    void shouldRejectApplyingToForeignDeck() {
        when(deckAccessGuard.owned(DECK_ID)).thenThrow(new DeckNotFoundException());

        assertThatThrownBy(() -> service.applyToDeck(DECK_ID, TEMPLATE_ID))
                .isInstanceOf(DeckNotFoundException.class);
        verify(templateRepository, never()).findByIdAndProfileId(anyLong(), anyLong());
    }

    @Test
    void shouldDeleteTemplateAndItsItems() {
        CategoryTemplate template = stubOwnedTemplate();

        service.delete(TEMPLATE_ID);

        verify(itemRepository).deleteByCategoryTemplateId(TEMPLATE_ID);
        verify(templateRepository).delete(template);
    }

    @Test
    void shouldListTemplatesForCurrentProfile() {
        CategoryTemplate template = new CategoryTemplate(PROFILE_ID, "Ramp Focus");
        ReflectionTestUtils.setField(template, "id", TEMPLATE_ID);
        when(templateRepository.findByProfileIdOrderByNameAsc(PROFILE_ID))
                .thenReturn(List.of(template));

        List<CategoryTemplateService.TemplateView> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Ramp Focus");
    }

    private CategoryTemplate stubOwnedTemplate() {
        CategoryTemplate template = new CategoryTemplate(PROFILE_ID, "Ramp Focus");
        ReflectionTestUtils.setField(template, "id", TEMPLATE_ID);
        when(templateRepository.findByIdAndProfileId(TEMPLATE_ID, PROFILE_ID))
                .thenReturn(Optional.of(template));
        return template;
    }

    private CategoryTemplate stubOwnedTemplateForApply() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(new Deck(PROFILE_ID, "Deck", "COMMANDER"));
        return stubOwnedTemplate();
    }
}
