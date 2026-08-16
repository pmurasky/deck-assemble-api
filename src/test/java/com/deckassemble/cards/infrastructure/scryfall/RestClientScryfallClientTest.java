package com.deckassemble.cards.infrastructure.scryfall;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RestClientScryfallClientTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
                "/cards/printing-id/rulings",
                exchange -> {
                    var body = payload().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
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
    void shouldFetchRulingCommentsByPrintingId() {
        var properties =
                new ScryfallProperties(
                        "http://localhost:" + server.getAddress().getPort(),
                        "test-agent",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Duration.ZERO);
        var client = new RestClientScryfallClient(properties, new ScryfallRateLimiter(properties));

        assertThat(client.getRulings("printing-id"))
                .containsExactly("Activate this ability only once each turn.");
    }

    private static String payload() {
        return """
                {"data":[{"comment":"Activate this ability only once each turn."}],"has_more":false}
                """;
    }
}
