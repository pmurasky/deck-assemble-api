package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCreateRequest;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.domain.DeckCard.Section;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import com.deckassemble.recommendations.domain.DeckBuild;
import com.deckassemble.recommendations.domain.DeckBuildRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

@Service
public class DeckBuilderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeckBuilderService.class);
    private static final String COMMANDER_FORMAT = "COMMANDER";
    private static final List<String> COLOR_ORDER = List.of("W", "U", "B", "R", "G");
    private static final Map<String, String> COLOR_TO_BASIC =
            Map.of(
                    "W", "Plains",
                    "U", "Island",
                    "B", "Swamp",
                    "R", "Mountain",
                    "G", "Forest");
    private static final Comparator<DeckCandidate> SCORE_ORDER =
            Comparator.comparing(DeckCandidate::hasScore)
                    .reversed()
                    .thenComparing(Comparator.comparingDouble(DeckCandidate::scoreValue).reversed())
                    .thenComparing(Comparator.comparingLong(DeckCandidate::inclusionValue).reversed())
                    .thenComparing(candidate -> candidate.card().getName());

    private final CardCatalogService cardCatalogService;
    private final CollectionService collectionService;
    private final EdhrecCommanderService edhrecCommanderService;
    private final CardCategorizer categorizer;
    private final DeckService deckService;
    private final DeckBuildRepository deckBuildRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUser currentUser;
    private final ProfileService profileService;

    // checkstyle:ParameterNumber suppressed: builder orchestrates the full build pipeline and
    // every collaborator is required; grouping them would add indirection without cohesion.
    @SuppressWarnings("checkstyle:ParameterNumber")
    public DeckBuilderService(
            CardCatalogService cardCatalogService,
            CollectionService collectionService,
            EdhrecCommanderService edhrecCommanderService,
            CardCategorizer categorizer,
            DeckService deckService,
            DeckBuildRepository deckBuildRepository,
            ObjectMapper objectMapper,
            CurrentUser currentUser,
            ProfileService profileService) {
        this.cardCatalogService = cardCatalogService;
        this.collectionService = collectionService;
        this.edhrecCommanderService = edhrecCommanderService;
        this.categorizer = categorizer;
        this.deckService = deckService;
        this.deckBuildRepository = deckBuildRepository;
        this.objectMapper = objectMapper;
        this.currentUser = currentUser;
        this.profileService = profileService;
    }

    public DeckBuildResult build(DeckBuildRequest request) {
        var profileId = profileId();
        var commanders = resolveCommanders(request);
        var identity = colorIdentity(commanders);
        var candidates = loadCandidates(profileId, commanders, identity);
        var targetSize = 100 - commanders.size();
        var picked = DeckDraftPicker.pick(candidates, targetSize);
        var gaps = new ArrayList<String>();
        var finalCards = padWithBasics(picked, identity, targetSize, gaps);
        var deck = createDeck(request, commanders);
        var counts = addCards(deck.id(), commanders, finalCards, gaps);
        var score = averageSynergy(picked);
        deckBuildRepository.save(new DeckBuild(deck.id(), configJson(request), score));
        return new DeckBuildResult(
                deck,
                finalCards.size() + commanders.size(),
                counts[0],
                counts[1],
                gaps,
                score,
                deckService.legality(deck.id()));
    }

    private List<Card> resolveCommanders(DeckBuildRequest request) {
        var commanders = new ArrayList<Card>();
        commanders.add(cardCatalogService.getCard(request.commanderCardId()));
        if (request.secondaryCommanderCardId() != null) {
            commanders.add(cardCatalogService.getCard(request.secondaryCommanderCardId()));
        }
        commanders.forEach(DeckBuilderService::requireEligible);
        return commanders;
    }

    private static void requireEligible(Card card) {
        var text = new StringBuilder();
        card.getFaces()
                .forEach(
                        face -> {
                            if (face.getTypeLine() != null) {
                                text.append(face.getTypeLine().toLowerCase()).append(' ');
                            }
                            if (face.getOracleText() != null) {
                                text.append(face.getOracleText().toLowerCase()).append(' ');
                            }
                        });
        var legendary = text.toString().contains("legendary") && text.toString().contains("creature");
        if (!legendary && !text.toString().contains("can be your commander")) {
            throw new IllegalArgumentException("Card is not eligible as commander: " + card.getName());
        }
    }

    private static Set<String> colorIdentity(List<Card> commanders) {
        var identity = new HashSet<String>();
        for (var commander : commanders) {
            if (commander.getColorIdentity() != null) {
                for (var color : commander.getColorIdentity().split(",")) {
                    if (!color.isBlank()) {
                        identity.add(color.trim());
                    }
                }
            }
        }
        return identity;
    }

    private List<DeckCandidate> loadCandidates(
            long profileId, List<Card> commanders, Set<String> identity) {
        var ownedPrintingIds = collectionService.getOwnedPrintingIds(profileId);
        var scores = loadScores(commanders.get(0));
        var commanderOracles =
                commanders.stream().map(Card::getScryfallOracleId).collect(Collectors.toSet());
        var seen = new HashSet<String>();
        var candidates = new ArrayList<DeckCandidate>();
        for (var entry : cardCatalogService.getCardsByPrintingIds(ownedPrintingIds).entrySet()) {
            var card = entry.getValue();
            if (isCandidate(card, commanderOracles, identity)
                    && seen.add(card.getScryfallOracleId())) {
                candidates.add(
                        new DeckCandidate(
                                entry.getKey(),
                                card,
                                categorizer.categorize(card),
                                scores.get(card.getName())));
            }
        }
        candidates.sort(SCORE_ORDER);
        return candidates;
    }

    private Map<String, CardScore> loadScores(Card commander) {
        try {
            return edhrecCommanderService.getCardScores(
                    commander.getScryfallOracleId(), commander.getName());
        } catch (RestClientException exception) {
            LOGGER.warn("EDHREC unavailable for {}; building without scores", commander.getName());
            return Map.of();
        }
    }

    private static boolean isCandidate(Card card, Set<String> commanderOracles, Set<String> identity) {
        return !commanderOracles.contains(card.getScryfallOracleId())
                && Boolean.TRUE.equals(card.getActive())
                && isCommanderLegal(card)
                && withinIdentity(card, identity);
    }

    private static boolean isCommanderLegal(Card card) {
        return card.getLegalities().stream()
                .anyMatch(
                        legality ->
                                "commander".equalsIgnoreCase(legality.getFormatCode())
                                        && "legal".equalsIgnoreCase(legality.getLegalityStatus()));
    }

    private static boolean withinIdentity(Card card, Set<String> identity) {
        if (card.getColorIdentity() == null || card.getColorIdentity().isBlank()) {
            return true;
        }
        for (var color : card.getColorIdentity().split(",")) {
            if (!color.isBlank() && !identity.contains(color.trim())) {
                return false;
            }
        }
        return true;
    }

    private List<DeckCandidate> padWithBasics(
            List<DeckCandidate> picked, Set<String> identity, int targetSize, List<String> gaps) {
        var cards = new ArrayList<>(picked);
        var missing = targetSize - picked.size();
        if (missing == 0) {
            return cards;
        }
        var basics = basicLands(identity);
        if (basics.isEmpty()) {
            gaps.add(missing + " slots could not be filled from your collection");
            return cards;
        }
        var names = new ArrayList<>(basics.keySet());
        for (var i = 0; i < missing; i++) {
            cards.add(basics.get(names.get(i % names.size())));
        }
        return cards;
    }

    private Map<String, DeckCandidate> basicLands(Set<String> identity) {
        var names = COLOR_ORDER.stream().filter(identity::contains).map(COLOR_TO_BASIC::get).toList();
        var cardsByName = new LinkedHashMap<String, Card>();
        cardCatalogService.getCardsByNames(names).forEach(card -> cardsByName.put(card.getName(), card));
        var printingIds =
                cardCatalogService.getLatestPrintingIdByCardIds(
                        cardsByName.values().stream().map(Card::getId).toList());
        var basics = new LinkedHashMap<String, DeckCandidate>();
        for (var name : names) {
            var card = cardsByName.get(name);
            var printingId = card != null ? printingIds.get(card.getId()) : null;
            if (card != null && printingId != null) {
                basics.put(name, new DeckCandidate(printingId, card, Category.LAND, null));
            }
        }
        return basics;
    }

    private DeckResponse createDeck(DeckBuildRequest request, List<Card> commanders) {
        return deckService.create(
                new DeckCreateRequest(
                        commanders.get(0).getName() + " EDHREC Build",
                        COMMANDER_FORMAT,
                        null,
                        commanders.get(0).getId(),
                        commanders.size() > 1 ? commanders.get(1).getId() : null,
                        true,
                        null,
                        request.desiredPowerLevel(),
                        request.playStyle()));
    }

    private int[] addCards(
            long deckId, List<Card> commanders, List<DeckCandidate> cards, List<String> gaps) {
        var counts = new int[2];
        addCommanders(deckId, commanders, counts, gaps);
        for (var candidate : cards) {
            var status =
                    deckService
                            .addCard(
                                    deckId, new DeckCardAddRequest(candidate.printingId(), 1, Section.MAIN_DECK))
                            .ownershipStatus();
            count(counts, status);
        }
        return counts;
    }

    private void addCommanders(
            long deckId, List<Card> commanders, int[] counts, List<String> gaps) {
        var printings =
                cardCatalogService.getLatestPrintingIdByCardIds(
                        commanders.stream().map(Card::getId).toList());
        for (var commander : commanders) {
            var printingId = printings.get(commander.getId());
            if (printingId == null) {
                gaps.add("Commander " + commander.getName() + " has no printing in the catalog");
                continue;
            }
            var status =
                    deckService
                            .addCard(deckId, new DeckCardAddRequest(printingId, 1, Section.COMMANDER))
                            .ownershipStatus();
            count(counts, status);
        }
    }

    private static void count(int[] counts, String ownershipStatus) {
        if ("WISHLIST".equals(ownershipStatus)) {
            counts[1]++;
        } else {
            counts[0]++;
        }
    }

    private static @Nullable BigDecimal averageSynergy(List<DeckCandidate> picked) {
        var total = 0.0;
        var count = 0;
        for (var candidate : picked) {
            var score = candidate.score();
            if (candidate.category() != Category.LAND && score != null && score.synergy() != null) {
                total += score.synergy();
                count++;
            }
        }
        return count == 0
                ? null
                : BigDecimal.valueOf(total / count).setScale(2, RoundingMode.HALF_UP);
    }

    private String configJson(DeckBuildRequest request) {
        return objectMapper.writeValueAsString(request);
    }

    private Long profileId() {
        String subject =
                currentUser.subject().orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return profileService.getOrCreate(subject).getId();
    }
}
