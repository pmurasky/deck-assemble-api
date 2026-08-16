package com.deckassemble.cards.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deckassemble.cards.domain.BeginnerGuideSource;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

class RestClientBeginnerGuideGeneratorTest {

    private static final String VALID_RESPONSE =
            "{\"summary\":\"Summary\",\"examples\":\"Examples\",\"whenToUse\":\"When\"}";

    private HttpServer server;
    private String requestBody;
    private int responseStatus = 200;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
                "/generate",
                exchange -> {
                    requestBody =
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8);
                    byte[] body = VALID_RESPONSE.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(responseStatus, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void shouldGenerateContentFromEveryOracleTextAndRuling() {
        var source =
                new BeginnerGuideSource(
                        "Spider-Man", List.of("Front text", "Back text"), List.of("Ruling one"));

        var content = client().generate(source);

        assertThat(content.summary()).isEqualTo("Summary");
        assertThat(requestBody).contains("Spider-Man", "Front text", "Back text", "Ruling one");
    }

    @Test
    void shouldPropagateLlmFailure() {
        responseStatus = 500;

        assertThatThrownBy(() -> client().generate(source()))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void shouldRejectMalformedGeneratedContent() {
        assertThatThrownBy(
                        () -> client().parse("{\"summary\":\"Summary\",\"examples\":\"Examples\"}"))
                .isInstanceOf(RestClientException.class)
                .hasMessage("Invalid beginner guide response");
    }

    private RestClientBeginnerGuideGenerator client() {
        return new RestClientBeginnerGuideGenerator(
                new BeginnerGuideLlmProperties(
                        "http://localhost:" + server.getAddress().getPort(),
                        "test",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1)),
                JsonMapper.builder().build());
    }

    private static BeginnerGuideSource source() {
        return new BeginnerGuideSource("Spider-Man", List.of("Text"), List.of());
    }
}
