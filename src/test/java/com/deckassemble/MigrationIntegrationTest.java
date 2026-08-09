package com.deckassemble;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyInteroperabilityAnalyticsMigration() {
        assertThat(countAppliedRelease("012-interoperability-analytics")).isOne();
        assertThat(tableExists("deck_import_previews")).isTrue();
        assertThat(tableExists("collection_import_previews")).isTrue();
        assertThat(
                        hasUniqueConstraint(
                                "deck_import_previews", "UNIQUE (profile_id, idempotency_key)"))
                .isTrue();
        assertThat(
                        hasUniqueConstraint(
                                "collection_import_previews",
                                "UNIQUE (profile_id, idempotency_key)"))
                .isTrue();
    }

    @Test
    void shouldApplyExperimentationPublishingMigration() {
        assertThat(countAppliedRelease("014-deck-revisions")).isOne();
        assertThat(tableExists("deck_revisions")).isTrue();
        assertThat(relationExists("idx_deck_revisions_deck_id")).isTrue();
        assertThat(relationExists("idx_deck_revisions_profile_id")).isTrue();
        assertThat(hasUniqueConstraint("deck_revisions", "UNIQUE (deck_id, revision_number)"))
                .isTrue();
    }

    @Test
    void shouldApplyCommunityPhysicalMigration() {
        assertThat(countAppliedRelease("015-deck-collaborators")).isOne();
        assertThat(countAppliedRelease("015-deck-comments")).isOne();
        assertThat(countAppliedRelease("015-profile-follows")).isOne();
        assertThat(countAppliedRelease("015-deck-favorites")).isOne();
        assertThat(countAppliedRelease("015-notifications")).isOne();
        assertThat(countAppliedRelease("015-moderation-reports")).isOne();

        assertThat(tableExists("deck_collaborators")).isTrue();
        assertThat(tableExists("deck_comments")).isTrue();
        assertThat(tableExists("profile_follows")).isTrue();
        assertThat(tableExists("deck_favorites")).isTrue();
        assertThat(tableExists("notifications")).isTrue();
        assertThat(tableExists("moderation_reports")).isTrue();

        assertThat(hasUniqueConstraint("deck_collaborators", "UNIQUE (deck_id, profile_id)"))
                .isTrue();
        assertThat(hasUniqueConstraint("profile_follows", "UNIQUE (follower_id, followee_id)"))
                .isTrue();
        assertThat(hasUniqueConstraint("deck_favorites", "UNIQUE (profile_id, deck_id)")).isTrue();

        assertThat(relationExists("idx_deck_collaborators_deck_id")).isTrue();
        assertThat(relationExists("idx_deck_comments_deck_id")).isTrue();
        assertThat(relationExists("idx_profile_follows_followee_id")).isTrue();
        assertThat(relationExists("idx_deck_favorites_deck_id")).isTrue();
        assertThat(relationExists("idx_notifications_recipient_read_at")).isTrue();
        assertThat(relationExists("idx_moderation_reports_status")).isTrue();
    }

    private int countAppliedRelease(String changeSetId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM databasechangelog WHERE id = ?", Integer.class, changeSetId);
    }

    private boolean tableExists(String tableName) {
        return relationExists(tableName);
    }

    private boolean relationExists(String relationName) {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        "SELECT to_regclass('public.' || ?) IS NOT NULL",
                        Boolean.class,
                        relationName));
    }

    private boolean hasUniqueConstraint(String tableName, String constraintDefinition) {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*) = 1
                        FROM pg_constraint c
                        JOIN pg_class t ON t.oid = c.conrelid
                        WHERE t.relname = ?
                          AND c.contype = 'u'
                          AND pg_get_constraintdef(c.oid) = ?
                        """,
                        Boolean.class,
                        tableName,
                        constraintDefinition));
    }
}
