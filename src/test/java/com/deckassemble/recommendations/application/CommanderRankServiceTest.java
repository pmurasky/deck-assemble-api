package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.recommendations.domain.EdhrecClient;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CommanderRankServiceTest {

    @Mock private EdhrecClient edhrecClient;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private CommanderRankRunRecorder runRecorder;

    @Test
    @SuppressWarnings("unchecked")
    void shouldAssignRanksInListOrderAndDeduplicate() {
        when(edhrecClient.fetchTopCommanders())
                .thenReturn(
                        payload(
                                """
                [{"name":"Atraxa, Praetors' Voice"},{"name":"The Ur-Dragon"},{"name":"Atraxa, Praetors' Voice"}]
                """));

        service().refreshCommanderRanks();

        var captor = ArgumentCaptor.forClass(Map.class);
        verify(cardCatalogService).updateCommanderRanks(captor.capture());
        assertThat(captor.getValue())
                .containsExactly(
                        Map.entry("Atraxa, Praetors' Voice", 1), Map.entry("The Ur-Dragon", 2));
    }

    @Test
    void shouldKeepExistingRanksWhenFetchFails() {
        when(edhrecClient.fetchTopCommanders()).thenThrow(new RestClientException("boom"));

        service().refreshCommanderRanks();

        verifyNoInteractions(cardCatalogService);
    }

    @Test
    void shouldKeepExistingRanksWhenListIsEmpty() {
        when(edhrecClient.fetchTopCommanders()).thenReturn(payload("[]"));

        service().refreshCommanderRanks();

        verifyNoInteractions(cardCatalogService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnCompletedOutcomeFromRefreshNow() {
        when(edhrecClient.fetchTopCommanders())
                .thenReturn(payload("""
                [{"name":"Atraxa, Praetors' Voice"},{"name":"The Ur-Dragon"}]
                """));
        when(cardCatalogService.updateCommanderRanks(any(Map.class))).thenReturn(2);
        when(runRecorder.start("manual")).thenReturn(7L);

        RefreshOutcome outcome = service().refreshNow("manual");

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.cardsUpdated()).isEqualTo(2);
        assertThat(outcome.errorSummary()).isNull();
        verify(runRecorder).complete(7L, 2);
    }

    @Test
    void shouldReturnFailedOutcomeWhenFetchFails() {
        when(edhrecClient.fetchTopCommanders()).thenThrow(new RestClientException("boom"));
        when(runRecorder.start("manual")).thenReturn(7L);

        RefreshOutcome outcome = service().refreshNow("manual");

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.errorSummary()).isEqualTo("boom");
        verify(runRecorder).fail(7L, "boom");
        verifyNoInteractions(cardCatalogService);
    }

    @Test
    void shouldReturnFailedOutcomeWhenListIsEmpty() {
        when(edhrecClient.fetchTopCommanders()).thenReturn(payload("[]"));
        when(runRecorder.start("manual")).thenReturn(7L);

        RefreshOutcome outcome = service().refreshNow("manual");

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.errorSummary()).isNotBlank();
        verify(runRecorder).fail(eq(7L), anyString());
        verifyNoInteractions(cardCatalogService);
    }

    private static String payload(String cardviews) {
        return """
                {"container":{"json_dict":{"cardlists":[{"cardviews":%s}]}}}
                """
                .formatted(cardviews);
    }

    private CommanderRankService service() {
        return new CommanderRankService(
                edhrecClient, cardCatalogService, JsonMapper.builder().build(), runRecorder);
    }
}
