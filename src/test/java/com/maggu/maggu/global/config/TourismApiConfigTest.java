package com.maggu.maggu.global.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourismApiConfigTest {

    private final TourismApiConfig config = new TourismApiConfig();

    @Nested
    @DisplayName("tourismApiRestClient의 readTimeout 적용")
    class ReadTimeout {

        @Test
        @DisplayName("서버가 readTimeout 이내에 응답하면 정상적으로 응답을 받는다")
        void succeedsWhenServerRespondsWithinReadTimeout() throws IOException {
            HttpServer server = startFakeServer(0);
            try {
                RestClient restClient = buildClient(server.getAddress().getPort(), Duration.ofSeconds(2));

                assertThatCode(() -> restClient.get().retrieve().toBodilessEntity())
                        .doesNotThrowAnyException();
            } finally {
                server.stop(0);
            }
        }

        @Test
        @DisplayName("서버가 readTimeout을 넘겨서 응답하면 ResourceAccessException을 던지고, readTimeout 근처에서 끊긴다")
        void throwsWhenServerExceedsReadTimeout() throws IOException {
            HttpServer server = startFakeServer(5_000);
            try {
                RestClient restClient = buildClient(server.getAddress().getPort(), Duration.ofSeconds(1));

                long start = System.currentTimeMillis();
                assertThatThrownBy(() -> restClient.get().retrieve().toBodilessEntity())
                        .isInstanceOf(ResourceAccessException.class);
                long elapsed = System.currentTimeMillis() - start;

                // 서버가 5초 응답 지연을 걸어도 readTimeout(1초) 근처에서 끊겨야 한다.
                assertThat(elapsed).isLessThan(3_000);
            } finally {
                server.stop(0);
            }
        }
    }

    private RestClient buildClient(int port, Duration readTimeout) {
        TourismApiProperties properties = new TourismApiProperties(
                "http://localhost:" + port, "test-key", Duration.ofSeconds(2), readTimeout);
        return config.tourismApiRestClient(properties);
    }

    private HttpServer startFakeServer(long responseDelayMs) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            try {
                if (responseDelayMs > 0) {
                    Thread.sleep(responseDelayMs);
                }
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException ignored) {
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }
}
