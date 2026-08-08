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
