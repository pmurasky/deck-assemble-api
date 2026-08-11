package com.deckassemble.community.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.organization.DeckCategory;
import com.deckassemble.decks.domain.organization.DeckCategoryRepository;
import com.deckassemble.decks.domain.organization.DeckTag;
import com.deckassemble.decks.domain.organization.DeckTagAssignment;
import com.deckassemble.decks.domain.organization.DeckTagAssignmentRepository;
import com.deckassemble.decks.domain.organization.DeckTagRepository;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class CommunityDiscoveryControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\\\"deckId\\\":(\\d+)");

    @Autowired private MockMvc mockMvc;
    @Autowired private DeckRepository deckRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private DeckCategoryRepository categoryRepository;
    @Autowired private DeckTagRepository tagRepository;
    @Autowired private DeckTagAssignmentRepository tagAssignmentRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldDiscoverOnlyPublishedPublicDecksAndExcludeUnlistedDecks() throws Exception {
        Profile owner = profile("auth|discover-public-owner");
        Deck publicDeck = deck(owner, "Public", DeckVisibility.PUBLIC);
        Deck unlisted = deck(owner, "Unlisted", DeckVisibility.UNLISTED);
        Deck draft = deck(owner, "Draft", DeckVisibility.PUBLIC);
        draft.setPublishedRevisionNumber(null);
        deckRepository.saveAndFlush(draft);
        DeckTag tag = tagRepository.saveAndFlush(new DeckTag(owner.getId(), "visibility-scope"));
        assignTag(publicDeck, tag);
        assignTag(unlisted, tag);
        assignTag(draft, tag);

        mockMvc.perform(get("/community/decks").param("tags", "visibility-scope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].deckId").value(publicDeck.getId()));
    }

    @Test
    void shouldFavoriteUnlistedDeckButStopListingItWhenPrivate() throws Exception {
        String owner = "auth|favorite-owner";
        long deckId = createDeck(owner, "Bookmark");
        String slug = publish(owner, deckId, DeckVisibility.UNLISTED);
        String viewer = "auth|favorite-viewer";

        mockMvc.perform(
                        post("/shared/decks/{slug}/favorite", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(viewer))))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/community/favorites").with(jwt().jwt(jwt -> jwt.subject(viewer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        publish(owner, deckId, DeckVisibility.PRIVATE);

        mockMvc.perform(get("/community/favorites").with(jwt().jwt(jwt -> jwt.subject(viewer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldFilterDiscoveryByCommanderColorsAndOwnerScopedTagName() throws Exception {
        Profile owner = profile("auth|discover-filter-owner");
        Card commander = card("Atraxa", "WUBG");
        Deck deck = deck(owner, "Filtered", DeckVisibility.PUBLIC);
        deck.setCommanderCardId(commander.getId());
        deckRepository.saveAndFlush(deck);
        DeckTag tag = tagRepository.saveAndFlush(new DeckTag(owner.getId(), "cEDH"));
        tagAssignmentRepository.saveAndFlush(new DeckTagAssignment(deck.getId(), tag.getId()));

        mockMvc.perform(get("/community/decks").param("colors", "WUBG").param("tags", "cedh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].deckId").value(deck.getId()));
    }

    @Test
    void shouldRejectTagAndCategoryAssignmentsOwnedByAnotherProfile() throws Exception {
        Profile owner = profile("auth|owner-scope-owner");
        Profile otherOwner = profile("auth|owner-scope-other-owner");
        Deck deck = deck(owner, "Owner Scoped", DeckVisibility.PUBLIC);
        DeckTag otherTag = tagRepository.saveAndFlush(new DeckTag(otherOwner.getId(), "Rogue"));
        tagAssignmentRepository.saveAndFlush(new DeckTagAssignment(deck.getId(), otherTag.getId()));
        categoryRepository.saveAndFlush(
                new DeckCategory(deck.getId(), otherOwner.getId(), "Rogue", 0, false));

        mockMvc.perform(get("/community/decks").param("tags", "rogue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(get("/community/decks").param("category", "rogue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldPageFavoritesAfterFilteringPrivateDecks() throws Exception {
        String owner = "auth|favorite-page-owner";
        String viewer = "auth|favorite-page-viewer";
        long visibleDeckId = createDeck(owner, "Visible Favorite");
        long staleDeckId = createDeck(owner, "Stale Favorite");
        String visibleSlug = publish(owner, visibleDeckId, DeckVisibility.PUBLIC);
        String staleSlug = publish(owner, staleDeckId, DeckVisibility.PUBLIC);

        favorite(viewer, visibleSlug);
        favorite(viewer, staleSlug);
        publish(owner, staleDeckId, DeckVisibility.PRIVATE);

        mockMvc.perform(
                        get("/community/favorites")
                                .with(jwt().jwt(jwt -> jwt.subject(viewer)))
                                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].deckId").value(visibleDeckId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldListFollowedPublicDecksNewestPublishedFirst() throws Exception {
        Profile followee = profile("auth|feed-followee");
        Profile outsider = profile("auth|feed-outsider");
        Deck older = deck(followee, "Older", DeckVisibility.PUBLIC);
        older.setPublishedAt(Instant.parse("2026-01-01T00:00:00Z"));
        Deck newer = deck(followee, "Newer", DeckVisibility.PUBLIC);
        newer.setPublishedAt(Instant.parse("2026-01-02T00:00:00Z"));
        Deck hidden = deck(followee, "Hidden", DeckVisibility.UNLISTED);
        hidden.setPublishedAt(Instant.parse("2026-01-03T00:00:00Z"));
        Deck notFollowed = deck(outsider, "Outsider", DeckVisibility.PUBLIC);
        notFollowed.setPublishedAt(Instant.parse("2026-01-04T00:00:00Z"));
        deckRepository.saveAllAndFlush(java.util.List.of(older, newer, hidden, notFollowed));
        String follower = "auth|feed-follower";

        mockMvc.perform(
                        post("/community/profiles/{profileId}/follow", followee.getId())
                                .with(jwt().jwt(jwt -> jwt.subject(follower))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/community/feed").with(jwt().jwt(jwt -> jwt.subject(follower))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].deckId").value(newer.getId()))
                .andExpect(jsonPath("$.content[1].deckId").value(older.getId()));
    }

    @Test
    void shouldKeepDiscoveryQueryCountBoundedForPageSize() throws Exception {
        Profile owner = profile("auth|query-count-owner");
        for (int i = 0; i < 5; i++) {
            deck(owner, "Deck " + i, DeckVisibility.PUBLIC);
        }
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        mockMvc.perform(get("/community/decks").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5));

        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(7L);
    }

    @Test
    void shouldFollowAndUnfollowProfilesIdempotently() throws Exception {
        Profile followee = profile("auth|followee");
        String follower = "auth|follower";

        mockMvc.perform(
                        post("/community/profiles/{profileId}/follow", followee.getId())
                                .with(jwt().jwt(jwt -> jwt.subject(follower))))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/community/profiles/{profileId}/follow", followee.getId())
                                .with(jwt().jwt(jwt -> jwt.subject(follower))))
                .andExpect(status().isOk());
        mockMvc.perform(
                        delete("/community/profiles/{profileId}/follow", followee.getId())
                                .with(jwt().jwt(jwt -> jwt.subject(follower))))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        delete("/community/profiles/{profileId}/follow", followee.getId())
                                .with(jwt().jwt(jwt -> jwt.subject(follower))))
                .andExpect(status().isNoContent());
    }

    private Profile profile(String subject) {
        return profileRepository.saveAndFlush(new Profile(subject, subject));
    }

    private Card card(String name, String colors) {
        Card card = new Card("oracle-" + System.nanoTime(), name);
        card.setColorIdentity(colors);
        return cardRepository.saveAndFlush(card);
    }

    private Deck deck(Profile owner, String name, DeckVisibility visibility) {
        Deck deck = new Deck(owner.getId(), name, "COMMANDER");
        deck.setVisibility(visibility);
        deck.setShareSlug("slug-" + System.nanoTime());
        deck.setPublishedRevisionNumber(1);
        return deckRepository.saveAndFlush(deck);
    }

    private void assignTag(Deck deck, DeckTag tag) {
        tagAssignmentRepository.saveAndFlush(new DeckTagAssignment(deck.getId(), tag.getId()));
    }

    private long createDeck(String subject, String name) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/decks")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType("application/json")
                                        .content(
                                                "{\"name\":\"%s\",\"formatCode\":\"COMMANDER\"}"
                                                        .formatted(name)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private String publish(String subject, long deckId, DeckVisibility visibility)
            throws Exception {
        MvcResult result =
                mockMvc.perform(
                                patch("/decks/{deckId}/publishing", deckId)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType("application/json")
                                        .content("{\"visibility\":\"%s\"}".formatted(visibility)))
                        .andExpect(status().isOk())
                        .andReturn();
        Matcher matcher =
                Pattern.compile("\"shareSlug\":\"([^\"]+)\"")
                        .matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private void favorite(String subject, String slug) throws Exception {
        mockMvc.perform(
                        post("/shared/decks/{slug}/favorite", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isCreated());
    }

    @SuppressWarnings("unused")
    private long firstDeckId(String json) {
        Matcher matcher = ID_PATTERN.matcher(json);
        assertThat(matcher.find()).isTrue();
        return Long.parseLong(matcher.group(1));
    }
}
