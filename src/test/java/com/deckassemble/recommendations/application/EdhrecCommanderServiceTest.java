package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.recommendations.domain.EdhrecClient;
import com.deckassemble.recommendations.domain.EdhrecCommanderCache;
import com.deckassemble.recommendations.domain.EdhrecCommanderCacheRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class EdhrecCommanderServiceTest {

    private static final String ORACLE_ID = "oracle-1";

    @Mock private EdhrecCommanderCacheRepository cacheRepository;
    @Mock private EdhrecClient edhrecClient;
    private EdhrecCommanderService service;

    @BeforeEach
    void setUp() {
        service =
                new EdhrecCommanderService(
                        cacheRepository, edhrecClient, JsonMapper.builder().build());
    }

    @Test
    void shouldReturnCachedPayloadWhenFresh() {
        var fresh = new EdhrecCommanderCache(ORACLE_ID, "cached-json", Instant.now());
        when(cacheRepository.findByCommanderOracleId(ORACLE_ID)).thenReturn(Optional.of(fresh));

        var result = service.getCommanderData(ORACLE_ID, "Atraxa, Praetors' Voice");

        assertThat(result).isEqualTo("cached-json");
        verifyNoInteractions(edhrecClient);
    }

    @Test
    void shouldFetchAndStoreWhenCacheIsMissing() {
        when(cacheRepository.findByCommanderOracleId(ORACLE_ID)).thenReturn(Optional.empty());
        when(edhrecClient.fetchCommanderData("atraxa-praetors-voice")).thenReturn("fetched-json");

        var result = service.getCommanderData(ORACLE_ID, "Atraxa, Praetors' Voice");

        assertThat(result).isEqualTo("fetched-json");
        var captor = ArgumentCaptor.forClass(EdhrecCommanderCache.class);
        verify(cacheRepository).save(captor.capture());
        assertThat(captor.getValue().getCommanderOracleId()).isEqualTo(ORACLE_ID);
        assertThat(captor.getValue().getPayload()).isEqualTo("fetched-json");
        assertThat(captor.getValue().getFetchedAt()).isNotNull();
    }

    @Test
    void shouldRefreshWhenCacheIsStale() {
        var stale =
                new EdhrecCommanderCache(
                        ORACLE_ID, "old-json", Instant.now().minus(8, ChronoUnit.DAYS));
        when(cacheRepository.findByCommanderOracleId(ORACLE_ID)).thenReturn(Optional.of(stale));
        when(edhrecClient.fetchCommanderData("atraxa-praetors-voice")).thenReturn("new-json");

        var result = service.getCommanderData(ORACLE_ID, "Atraxa, Praetors' Voice");

        assertThat(result).isEqualTo("new-json");
        assertThat(stale.getPayload()).isEqualTo("new-json");
        verify(cacheRepository).save(stale);
    }

    @Test
    void shouldServeStaleCacheWhenFetchFails() {
        var stale =
                new EdhrecCommanderCache(
                        ORACLE_ID, "old-json", Instant.now().minus(8, ChronoUnit.DAYS));
        when(cacheRepository.findByCommanderOracleId(ORACLE_ID)).thenReturn(Optional.of(stale));
        when(edhrecClient.fetchCommanderData("atraxa-praetors-voice"))
                .thenThrow(new RestClientException("edhrec down"));

        var result = service.getCommanderData(ORACLE_ID, "Atraxa, Praetors' Voice");

        assertThat(result).isEqualTo("old-json");
        verify(cacheRepository, never()).save(stale);
    }

    @Test
    void shouldPropagateFailureWhenNoCacheExists() {
        when(cacheRepository.findByCommanderOracleId(ORACLE_ID)).thenReturn(Optional.empty());
        when(edhrecClient.fetchCommanderData("atraxa-praetors-voice"))
                .thenThrow(new RestClientException("edhrec down"));

        assertThatThrownBy(() -> service.getCommanderData(ORACLE_ID, "Atraxa, Praetors' Voice"))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void shouldMergeCardScoresAcrossCardlists() {
        var payload =
                """
                {"container":{"json_dict":{"cardlists":[
                  {"cardviews":[{"name":"Sol Ring","synergy":0.4,"inclusion":100},
                                {"name":"Arcane Signet","inclusion":50}]},
                  {"cardviews":[{"name":"Sol Ring","synergy":0.6,"inclusion":80}]}
                ]}}}""";
        var fresh = new EdhrecCommanderCache(ORACLE_ID, payload, Instant.now());
        when(cacheRepository.findByCommanderOracleId(ORACLE_ID)).thenReturn(Optional.of(fresh));

        var scores = service.getCardScores(ORACLE_ID, "Atraxa, Praetors' Voice");

        assertThat(scores.get("Sol Ring").synergy()).isEqualTo(0.6);
        assertThat(scores.get("Sol Ring").inclusion()).isEqualTo(100L);
        assertThat(scores.get("Arcane Signet").synergy()).isNull();
        assertThat(scores.get("Arcane Signet").inclusion()).isEqualTo(50L);
    }

    @Test
    void shouldCaptureCardlistHeadersOnScores() {
        var payload =
                """
                {"container":{"json_dict":{"cardlists":[
                  {"header":"Combo","cardviews":[{"name":"Sol Ring","synergy":0.4,"inclusion":100}]},
                  {"header":"Top Cards","cardviews":[{"name":"Sol Ring","synergy":0.6}]}
                ]}}}""";
        var fresh = new EdhrecCommanderCache(ORACLE_ID, payload, Instant.now());
        when(cacheRepository.findByCommanderOracleId(ORACLE_ID)).thenReturn(Optional.of(fresh));

        var scores = service.getCardScores(ORACLE_ID, "Atraxa, Praetors' Voice");

        assertThat(scores.get("Sol Ring").synergy()).isEqualTo(0.6);
        assertThat(scores.get("Sol Ring").cardlists())
                .containsExactlyInAnyOrder("Combo", "Top Cards");
    }

    @Test
    void shouldSkipCardviewsWithoutNames() {
        var payload =
                """
                {"container":{"json_dict":{"cardlists":[
                  {"cardviews":[{"synergy":0.4},{"name":"Sol Ring","synergy":0.2}]}
                ]}}}""";
        var fresh = new EdhrecCommanderCache(ORACLE_ID, payload, Instant.now());
        when(cacheRepository.findByCommanderOracleId(ORACLE_ID)).thenReturn(Optional.of(fresh));

        var scores = service.getCardScores(ORACLE_ID, "Atraxa, Praetors' Voice");

        assertThat(scores).containsOnlyKeys("Sol Ring");
    }

    @Test
    void shouldConvertCommanderNamesToSlugs() {
        assertThat(EdhrecCommanderService.toSlug("Atraxa, Praetors' Voice"))
                .isEqualTo("atraxa-praetors-voice");
        assertThat(EdhrecCommanderService.toSlug("K'rrik, Son of Yawgmoth"))
                .isEqualTo("krrik-son-of-yawgmoth");
        assertThat(EdhrecCommanderService.toSlug("Yuriko, the Tiger's Shadow"))
                .isEqualTo("yuriko-the-tigers-shadow");
    }
}
