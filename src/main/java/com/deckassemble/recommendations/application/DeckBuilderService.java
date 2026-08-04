package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCardService;
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
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

// Justified: orchestration service coordinating the deck-build pipeline; decomposition tracked in
// #3.
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.TooManyMethods"})
@Service
public class DeckBuilderService {

    private static final String COMMANDER_FORMAT = "COMMANDER";
    private static final int DECK_SIZE = 100;
    private final CardCatalogService cardCatalogService;
    private final CommanderResolver commanderResolver;
    private final DeckCandidateSelector deckCandidateSelector;
    private final BasicLandPadder basicLandPadder;
    private final DeckService deckService;
    private final DeckCardService deckCardService;
    private final DeckBuildRepository deckBuildRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUser currentUser;
    private final ProfileService profileService;

    // checkstyle:ParameterNumber suppressed: builder orchestrates the full build pipeline and
    // every collaborator is required; grouping them would add indirection without cohesion.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public DeckBuilderService(
            CardCatalogService cardCatalogService,
            CommanderResolver commanderResolver,
            DeckCandidateSelector deckCandidateSelector,
            BasicLandPadder basicLandPadder,
            DeckService deckService,
            DeckCardService deckCardService,
            DeckBuildRepository deckBuildRepository,
            ObjectMapper objectMapper,
            CurrentUser currentUser,
            ProfileService profileService) {
        this.cardCatalogService = cardCatalogService;
        this.commanderResolver = commanderResolver;
        this.deckCandidateSelector = deckCandidateSelector;
        this.basicLandPadder = basicLandPadder;
        this.deckService = deckService;
        this.deckCardService = deckCardService;
        this.deckBuildRepository = deckBuildRepository;
        this.objectMapper = objectMapper;
        this.currentUser = currentUser;
        this.profileService = profileService;
    }

    public DeckBuildResult build(DeckBuildRequest request) {
        var profileId = profileId();
        var commanders = commanderResolver.resolve(request);
        var identity = CommanderResolver.colorIdentity(commanders);
        var candidates = deckCandidateSelector.select(request, commanders, identity, profileId);
        return assembleDeck(request, commanders, identity, candidates);
    }

    private DeckBuildResult assembleDeck(
            DeckBuildRequest request,
            List<Card> commanders,
            Set<String> identity,
            List<DeckCandidate> candidates) {
        var gaps = new ArrayList<String>();
        var finalCards = draftMainDeck(candidates, identity, DECK_SIZE - commanders.size(), gaps);
        var deck = createDeck(request, commanders);
        var counts = addCards(deck.id(), commanders, finalCards, gaps);
        var score = averageSynergy(finalCards);
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

    private List<DeckCandidate> draftMainDeck(
            List<DeckCandidate> candidates,
            Set<String> identity,
            int targetSize,
            List<String> gaps) {
        var picked = DeckDraftPicker.pick(candidates, targetSize);
        return basicLandPadder.pad(picked, identity, targetSize, gaps);
    }

    private DeckResponse createDeck(DeckBuildRequest request, List<Card> commanders) {
        return deckService.create(
                new DeckCreateRequest(
                        commanders.get(0).getName() + " EDHREC Build",
                        COMMANDER_FORMAT,
                        null,
                        commanders.get(0).getId(),
                        commanders.size() > 1 ? commanders.get(1).getId() : null,
                        DeckCandidateSelector.ownedOnly(request),
                        request.budgetLimit(),
                        request.desiredPowerLevel(),
                        request.playStyle()));
    }

    private int[] addCards(
            long deckId, List<Card> commanders, List<DeckCandidate> cards, List<String> gaps) {
        var counts = new int[2];
        addCommanders(deckId, commanders, counts, gaps);
        for (var candidate : cards) {
            var status =
                    deckCardService
                            .addCard(
                                    deckId,
                                    new DeckCardAddRequest(
                                            candidate.printingId(), 1, Section.MAIN_DECK))
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
                    deckCardService
                            .addCard(
                                    deckId,
                                    new DeckCardAddRequest(printingId, 1, Section.COMMANDER))
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
                currentUser
                        .subject()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return profileService.getOrCreate(subject).getId();
    }
}
