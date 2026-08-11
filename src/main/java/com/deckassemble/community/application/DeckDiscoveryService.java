package com.deckassemble.community.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.community.domain.DeckFavorite;
import com.deckassemble.community.domain.DeckFavoriteRepository;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.organization.DeckCategory;
import com.deckassemble.decks.domain.organization.DeckTag;
import com.deckassemble.decks.domain.organization.DeckTagAssignment;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",
    "PMD.CyclomaticComplexity",
    "PMD.TooManyMethods",
    "PMD.UseConcurrentHashMap"
})
public class DeckDiscoveryService {

    private static final Map<String, String> SORT_FIELDS =
            Map.of(
                    "name", "name",
                    "updated", "updatedAt",
                    "updatedAt", "updatedAt",
                    "published", "publishedAt",
                    "publishedAt", "publishedAt");

    private final DeckRepository deckRepository;
    private final DeckFavoriteRepository favoriteRepository;
    private final CardCatalogService cardCatalogService;
    private final CurrentUser currentUser;
    private final ProfileService profileService;

    public DeckDiscoveryService(
            DeckRepository deckRepository,
            DeckFavoriteRepository favoriteRepository,
            CardCatalogService cardCatalogService,
            CurrentUser currentUser,
            ProfileService profileService) {
        this.deckRepository = deckRepository;
        this.favoriteRepository = favoriteRepository;
        this.cardCatalogService = cardCatalogService;
        this.currentUser = currentUser;
        this.profileService = profileService;
    }

    public Page<Item> discover(Query query, Pageable pageable) {
        Long viewerProfileId = viewerProfileId();
        Page<Deck> page =
                deckRepository.findAll(spec(query, viewerProfileId), safePageable(pageable));
        Decorations decorations = decorations(page, viewerProfileId);
        return page.map(deck -> item(deck, decorations));
    }

    private Item item(Deck deck, Decorations decorations) {
        return new Item(
                deck,
                decorations.counts().getOrDefault(deck.getId(), 0L),
                decorations.viewerFavorites().contains(deck.getId()));
    }

    private Decorations decorations(Page<Deck> page, @Nullable Long viewerProfileId) {
        Set<Long> deckIds =
                page.stream().map(Deck::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (deckIds.isEmpty()) {
            return new Decorations(Map.of(), Set.of());
        }
        return new Decorations(favoriteCounts(deckIds), viewerFavorites(viewerProfileId, deckIds));
    }

    private Map<Long, Long> favoriteCounts(Set<Long> deckIds) {
        Map<Long, Long> counts = new HashMap<>();
        favoriteRepository
                .countByDeckIds(deckIds)
                .forEach(count -> counts.put(count.getDeckId(), count.getFavoriteCount()));
        return counts;
    }

    private Set<Long> viewerFavorites(@Nullable Long viewerProfileId, Set<Long> deckIds) {
        if (viewerProfileId == null) {
            return Set.of();
        }
        return favoriteRepository.findDeckIdsByProfileIdAndDeckIdIn(viewerProfileId, deckIds);
    }

    private Specification<Deck> spec(Query query, @Nullable Long viewerProfileId) {
        Specification<Deck> spec = publicPublished();
        spec = spec.and(commanderSpec(query.commander())).and(colorsSpec(query.colors()));
        spec = spec.and(tagsSpec(query.tags())).and(categorySpec(query.category()));
        spec = spec.and(updatedSpec(query)).and(favoriteSpec(query.favorited(), viewerProfileId));
        return spec;
    }

    private Specification<Deck> publicPublished() {
        return (root, criteria, builder) ->
                builder.and(
                        builder.equal(root.get("visibility"), DeckVisibility.PUBLIC),
                        builder.isNotNull(root.get("publishedRevisionNumber")));
    }

    private Specification<Deck> commanderSpec(@Nullable String commander) {
        String name = blankToNull(commander);
        if (name == null) {
            return noOp();
        }
        Set<Long> ids =
                cardCatalogService.getCardsByNames(List.of(name)).stream()
                        .map(Card::getId)
                        .collect(Collectors.toSet());
        return ids.isEmpty()
                ? impossible()
                : (root, criteria, builder) -> root.get("commanderCardId").in(ids);
    }

    private Specification<Deck> colorsSpec(Collection<String> colors) {
        Set<String> normalized = normalizedValues(colors);
        if (normalized.isEmpty()) {
            return noOp();
        }
        Set<Long> ids = cardCatalogService.getActiveCardIdsByColorIdentity(normalized);
        return ids.isEmpty()
                ? impossible()
                : (root, criteria, builder) -> root.get("commanderCardId").in(ids);
    }

    private Specification<Deck> tagsSpec(Collection<String> tags) {
        Set<String> names = normalizedValues(tags);
        if (names.isEmpty()) {
            return noOp();
        }
        Specification<Deck> spec = noOp();
        for (String name : names) {
            spec = spec.and(tagSpec(name));
        }
        return spec;
    }

    private Specification<Deck> tagSpec(String tagName) {
        return (root, criteria, builder) -> {
            Subquery<Long> subquery = criteria.subquery(Long.class);
            Root<DeckTagAssignment> assignment = subquery.from(DeckTagAssignment.class);
            Root<DeckTag> tag = subquery.from(DeckTag.class);
            subquery.select(assignment.get("id"));
            subquery.where(
                    builder.equal(assignment.get("deckId"), root.get("id")),
                    builder.equal(assignment.get("tagId"), tag.get("id")),
                    builder.equal(tag.get("profileId"), root.get("profileId")),
                    builder.equal(builder.lower(tag.get("name")), tagName));
            return builder.exists(subquery);
        };
    }

    private Specification<Deck> categorySpec(@Nullable String category) {
        String name = blankToNull(category);
        if (name == null) {
            return noOp();
        }
        return (root, criteria, builder) -> {
            Subquery<Long> subquery = criteria.subquery(Long.class);
            Root<DeckCategory> deckCategory = subquery.from(DeckCategory.class);
            subquery.select(deckCategory.get("id"));
            subquery.where(
                    builder.equal(deckCategory.get("deckId"), root.get("id")),
                    builder.equal(
                            builder.lower(deckCategory.get("name")),
                            name.toLowerCase(Locale.ROOT)));
            return builder.exists(subquery);
        };
    }

    private Specification<Deck> updatedSpec(Query query) {
        return (root, criteria, builder) -> {
            var predicate = builder.conjunction();
            if (query.updatedAfter() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.greaterThanOrEqualTo(
                                        root.get("updatedAt"), query.updatedAfter()));
            }
            if (query.updatedBefore() != null) {
                predicate =
                        builder.and(
                                predicate,
                                builder.lessThanOrEqualTo(
                                        root.get("updatedAt"), query.updatedBefore()));
            }
            return predicate;
        };
    }

    private Specification<Deck> favoriteSpec(
            @Nullable Boolean favorited, @Nullable Long viewerProfileId) {
        if (favorited == null) {
            return noOp();
        }
        if (viewerProfileId == null) {
            return Boolean.TRUE.equals(favorited) ? impossible() : noOp();
        }
        return (root, criteria, builder) -> {
            Subquery<Long> subquery = criteria.subquery(Long.class);
            Root<DeckFavorite> favorite = subquery.from(DeckFavorite.class);
            subquery.select(favorite.get("deckId"));
            subquery.where(
                    builder.equal(favorite.get("deckId"), root.get("id")),
                    builder.equal(favorite.get("profileId"), viewerProfileId));
            return Boolean.TRUE.equals(favorited)
                    ? builder.exists(subquery)
                    : builder.not(builder.exists(subquery));
        };
    }

    private Specification<Deck> impossible() {
        return (root, criteria, builder) -> builder.disjunction();
    }

    private Specification<Deck> noOp() {
        return (root, criteria, builder) -> builder.conjunction();
    }

    private Pageable safePageable(Pageable pageable) {
        Sort sort = translateSort(pageable.getSort());
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort.and(Sort.by("id").ascending()));
    }

    private Sort translateSort(Sort requested) {
        if (requested.isUnsorted()) {
            return Sort.by(Sort.Order.desc("updatedAt"));
        }
        return Sort.by(requested.stream().map(this::translateOrder).toList());
    }

    private Sort.Order translateOrder(Sort.Order order) {
        String property = SORT_FIELDS.get(order.getProperty());
        if (property == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        return new Sort.Order(order.getDirection(), property);
    }

    private @Nullable Long viewerProfileId() {
        return currentUser
                .subject()
                .flatMap(profileService::findBySubject)
                .map(profile -> profile.getId())
                .orElse(null);
    }

    private Set<String> normalizedValues(Collection<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(this::blankToNull)
                .filter(value -> value != null)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record Decorations(Map<Long, Long> counts, Set<Long> viewerFavorites) {}

    public record Query(
            @Nullable String commander,
            Collection<String> colors,
            Collection<String> tags,
            @Nullable String category,
            @Nullable Instant updatedAfter,
            @Nullable Instant updatedBefore,
            @Nullable Boolean favorited) {}

    public record Item(Deck deck, long favoriteCount, boolean favoritedByViewer) {}
}
