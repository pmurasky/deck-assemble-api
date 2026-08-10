package com.deckassemble.imports.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.ScryfallClient;
import com.deckassemble.shared.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OracleTagImportServiceTest {

    @Mock private ScryfallClient scryfallClient;
    @Mock private CardRepository cardRepository;
    @Mock private ImportRunRecorder runRecorder;
    @Mock private CurrentUser currentUser;

    private OracleTagImportService service;

    @BeforeEach
    void setUp() {
        service =
                new OracleTagImportService(
                        scryfallClient, cardRepository, runRecorder, currentUser);
    }

    @Test
    void shouldStoreSortedCommaSeparatedTagsOnTaggedCards() {
        // Given a tagged card present in the catalog
        Card card = new Card("oracle-1", "Cultivate");
        when(scryfallClient.fetchOracleTagAssignments())
                .thenReturn(Map.of("oracle-1", Set.of("ramp", "landfall")));
        when(cardRepository.findAll()).thenReturn(List.of(card));

        // When importing tags
        ImportResult result = service.importTags(7L);

        // Then the card carries the sorted label list and the run reflects the update
        assertThat(card.getOracleTags()).isEqualTo("landfall,ramp");
        verify(cardRepository).saveAll(List.of(card));
        assertThat(result.recordsUpdated()).isEqualTo(1);
    }

    @Test
    void shouldSkipIndexEntriesForCardsNotInCatalog() {
        // Given the tag index references a card we have not imported
        when(scryfallClient.fetchOracleTagAssignments())
                .thenReturn(Map.of("oracle-unknown", Set.of("ramp")));
        when(cardRepository.findAll()).thenReturn(List.of());

        // When importing tags
        ImportResult result = service.importTags(7L);

        // Then the entry is counted as skipped, not created
        assertThat(result.recordsFailed()).isEqualTo(1);
        assertThat(result.recordsUpdated()).isZero();
    }

    @Test
    void shouldNotResaveCardsWhoseTagsAreUnchanged() {
        // Given a card already carrying the same tags
        Card card = new Card("oracle-1", "Cultivate");
        card.setOracleTags("landfall,ramp");
        when(scryfallClient.fetchOracleTagAssignments())
                .thenReturn(Map.of("oracle-1", Set.of("ramp", "landfall")));
        when(cardRepository.findAll()).thenReturn(List.of(card));

        // When importing tags
        ImportResult result = service.importTags(7L);

        // Then nothing is written
        assertThat(result.recordsUpdated()).isZero();
    }

    @Test
    void shouldStartRunWhenImportingWithoutRunId() {
        // Given an interactive import
        when(currentUser.subject()).thenReturn(Optional.of("admin"));
        when(runRecorder.start("tagger:oracle-tags", "admin")).thenReturn(42L);
        when(scryfallClient.fetchOracleTagAssignments()).thenReturn(Map.of());
        when(cardRepository.findAll()).thenReturn(List.of());

        // When importing tags
        ImportResult result = service.importTags();

        // Then a run was started and used
        assertThat(result.runId()).isEqualTo(42L);
    }

    @Test
    void shouldRecordRunFailureWhenFetchFails() {
        // Given the provider is down
        when(scryfallClient.fetchOracleTagAssignments())
                .thenThrow(new IllegalStateException("boom"));

        // When importing tags
        assertThatThrownBy(() -> service.importTags(7L)).isInstanceOf(IllegalStateException.class);

        // Then the run is marked failed
        verify(runRecorder).fail(anyLong(), anyString());
    }
}
