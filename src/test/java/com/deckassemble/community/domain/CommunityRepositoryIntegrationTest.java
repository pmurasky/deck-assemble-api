package com.deckassemble.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CommunityRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired private DeckCommentRepository commentRepository;
    @Autowired private ProfileFollowRepository followRepository;
    @Autowired private DeckFavoriteRepository favoriteRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ModerationReportRepository moderationReportRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void shouldCascadeDeleteCommentsWhenDeckIsDeleted() {
        Deck deck = saveDeck();
        Profile author = saveProfile();
        commentRepository.saveAndFlush(new DeckComment(deck.getId(), author.getId(), "Nice deck!"));

        deckRepository.delete(deck);
        deckRepository.flush();
        entityManager.clear();

        assertThat(commentRepository.findByDeckIdOrderByCreatedAtDesc(deck.getId())).isEmpty();
    }

    @Test
    void shouldFindCommentsByDeckMostRecentFirst() {
        Deck deck = saveDeck();
        Profile author = saveProfile();
        commentRepository.saveAndFlush(new DeckComment(deck.getId(), author.getId(), "First"));
        commentRepository.saveAndFlush(new DeckComment(deck.getId(), author.getId(), "Second"));

        assertThat(commentRepository.findByDeckIdOrderByCreatedAtDesc(deck.getId()))
                .extracting(DeckComment::getBody)
                .containsExactly("Second", "First");
    }

    @Test
    void shouldEditCommentBody() {
        Deck deck = saveDeck();
        Profile author = saveProfile();
        DeckComment comment =
                commentRepository.saveAndFlush(
                        new DeckComment(deck.getId(), author.getId(), "Original"));

        comment.editBody("Edited");
        commentRepository.saveAndFlush(comment);
        entityManager.clear();

        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getBody())
                .isEqualTo("Edited");
    }

    @Test
    void shouldRejectDuplicateFollowOfSameFollowee() {
        Profile follower = saveProfile();
        Profile followee = saveProfile();
        followRepository.saveAndFlush(new ProfileFollow(follower.getId(), followee.getId()));

        assertThatThrownBy(
                        () ->
                                followRepository.saveAndFlush(
                                        new ProfileFollow(follower.getId(), followee.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCascadeDeleteFollowsWhenFollowerIsDeleted() {
        Profile follower = saveProfile();
        Profile followee = saveProfile();
        followRepository.saveAndFlush(new ProfileFollow(follower.getId(), followee.getId()));

        profileRepository.delete(follower);
        profileRepository.flush();
        entityManager.clear();

        assertThat(followRepository.findByFolloweeId(followee.getId())).isEmpty();
    }

    @Test
    void shouldReportFollowExistence() {
        Profile follower = saveProfile();
        Profile followee = saveProfile();

        assertThat(
                        followRepository.existsByFollowerIdAndFolloweeId(
                                follower.getId(), followee.getId()))
                .isFalse();

        followRepository.saveAndFlush(new ProfileFollow(follower.getId(), followee.getId()));

        assertThat(
                        followRepository.existsByFollowerIdAndFolloweeId(
                                follower.getId(), followee.getId()))
                .isTrue();
    }

    @Test
    void shouldRejectDuplicateFavoriteOfSameDeck() {
        Profile profile = saveProfile();
        Deck deck = saveDeck();
        favoriteRepository.saveAndFlush(new DeckFavorite(profile.getId(), deck.getId()));

        assertThatThrownBy(
                        () ->
                                favoriteRepository.saveAndFlush(
                                        new DeckFavorite(profile.getId(), deck.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCascadeDeleteFavoritesWhenDeckIsDeleted() {
        Profile profile = saveProfile();
        Deck deck = saveDeck();
        favoriteRepository.saveAndFlush(new DeckFavorite(profile.getId(), deck.getId()));

        deckRepository.delete(deck);
        deckRepository.flush();
        entityManager.clear();

        assertThat(favoriteRepository.findByProfileId(profile.getId())).isEmpty();
    }

    @Test
    void shouldReportFavoriteExistence() {
        Profile profile = saveProfile();
        Deck deck = saveDeck();

        assertThat(favoriteRepository.existsByProfileIdAndDeckId(profile.getId(), deck.getId()))
                .isFalse();

        favoriteRepository.saveAndFlush(new DeckFavorite(profile.getId(), deck.getId()));

        assertThat(favoriteRepository.existsByProfileIdAndDeckId(profile.getId(), deck.getId()))
                .isTrue();
    }

    @Test
    void shouldFindOnlyUnreadNotificationsForRecipient() {
        Profile recipient = saveProfile();
        Profile actor = saveProfile();
        Notification unread =
                notificationRepository.saveAndFlush(
                        new Notification(
                                recipient.getId(),
                                actor.getId(),
                                Notification.Reason.NEW_FOLLOWER,
                                actor.getId().toString()));
        Notification read =
                notificationRepository.saveAndFlush(
                        new Notification(
                                recipient.getId(),
                                actor.getId(),
                                Notification.Reason.NEW_FOLLOWER,
                                actor.getId().toString()));
        read.markRead(Instant.now());
        notificationRepository.saveAndFlush(read);

        assertThat(
                        notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(
                                recipient.getId()))
                .extracting(Notification::getId)
                .containsExactly(unread.getId());
    }

    @Test
    void shouldCountUnreadNotificationsForRecipient() {
        Profile recipient = saveProfile();
        Profile actor = saveProfile();
        Notification notification =
                notificationRepository.saveAndFlush(
                        new Notification(
                                recipient.getId(),
                                actor.getId(),
                                Notification.Reason.NEW_FOLLOWER,
                                actor.getId().toString()));
        assertThat(notification.isUnread()).isTrue();

        assertThat(notificationRepository.countByRecipientIdAndReadAtIsNull(recipient.getId()))
                .isEqualTo(1L);

        notification.markRead(Instant.now());
        notificationRepository.saveAndFlush(notification);

        assertThat(notification.isUnread()).isFalse();
        assertThat(notificationRepository.countByRecipientIdAndReadAtIsNull(recipient.getId()))
                .isZero();
    }

    @Test
    void shouldRetainNotificationAndClearActorWhenActorIsDeleted() {
        Profile recipient = saveProfile();
        Profile actor = saveProfile();
        Notification notification =
                notificationRepository.saveAndFlush(
                        new Notification(
                                recipient.getId(),
                                actor.getId(),
                                Notification.Reason.NEW_COMMENT,
                                UUID.randomUUID().toString()));

        profileRepository.delete(actor);
        profileRepository.flush();
        entityManager.clear();

        Notification reloaded = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(reloaded.getActorId()).isNull();
    }

    @Test
    void shouldCascadeDeleteNotificationsWhenRecipientIsDeleted() {
        Profile recipient = saveProfile();
        Profile actor = saveProfile();
        notificationRepository.saveAndFlush(
                new Notification(
                        recipient.getId(),
                        actor.getId(),
                        Notification.Reason.NEW_COMMENT,
                        UUID.randomUUID().toString()));

        profileRepository.delete(recipient);
        profileRepository.flush();
        entityManager.clear();

        assertThat(
                        notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(
                                recipient.getId()))
                .isEmpty();
    }

    @Test
    void shouldRetainModerationReportAndClearReporterWhenReporterIsDeleted() {
        Profile reporter = saveProfile();
        ModerationReport report =
                moderationReportRepository.saveAndFlush(
                        new ModerationReport(
                                reporter.getId(),
                                ModerationReport.ResourceType.DECK,
                                UUID.randomUUID().toString(),
                                ModerationReport.Reason.SPAM,
                                "This deck is spam"));

        profileRepository.delete(reporter);
        profileRepository.flush();
        entityManager.clear();

        ModerationReport reloaded =
                moderationReportRepository.findById(report.getId()).orElseThrow();
        assertThat(reloaded.getReporterId()).isNull();
    }

    @Test
    void shouldTransitionModerationReportStatus() {
        Profile reporter = saveProfile();
        ModerationReport report =
                moderationReportRepository.saveAndFlush(
                        new ModerationReport(
                                reporter.getId(),
                                ModerationReport.ResourceType.PROFILE,
                                UUID.randomUUID().toString(),
                                ModerationReport.Reason.OTHER,
                                null));
        assertThat(report.getStatus()).isEqualTo(ModerationReport.Status.OPEN);

        report.resolve();
        moderationReportRepository.saveAndFlush(report);
        assertThat(moderationReportRepository.findById(report.getId()).orElseThrow().getStatus())
                .isEqualTo(ModerationReport.Status.RESOLVED);

        report.dismiss();
        moderationReportRepository.saveAndFlush(report);
        assertThat(moderationReportRepository.findById(report.getId()).orElseThrow().getStatus())
                .isEqualTo(ModerationReport.Status.DISMISSED);
    }

    @Test
    void shouldFindModerationReportsByStatus() {
        Profile reporter = saveProfile();
        ModerationReport report =
                moderationReportRepository.saveAndFlush(
                        new ModerationReport(
                                reporter.getId(),
                                ModerationReport.ResourceType.COMMENT,
                                UUID.randomUUID().toString(),
                                ModerationReport.Reason.HARASSMENT,
                                null));

        assertThat(moderationReportRepository.findByStatus(ModerationReport.Status.OPEN))
                .extracting(ModerationReport::getId)
                .contains(report.getId());
        assertThat(moderationReportRepository.findByStatus(ModerationReport.Status.RESOLVED))
                .isEmpty();
    }

    private Deck saveDeck() {
        Profile owner = profileRepository.save(new Profile("auth|" + System.nanoTime(), "Owner"));
        return deckRepository.save(new Deck(owner.getId(), "Test Deck", "commander"));
    }

    private Profile saveProfile() {
        return profileRepository.save(new Profile("auth|" + System.nanoTime(), "Member"));
    }
}
