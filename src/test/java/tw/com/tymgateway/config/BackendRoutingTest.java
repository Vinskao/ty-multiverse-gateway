package tw.com.tymgateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.junit.jupiter.api.AfterAll;
import reactor.netty.http.server.HttpServer;
import reactor.netty.DisposableServer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendRoutingTest {
    private static final String TCX = "/tymb/health-data/workout-tcx";
    private static final DisposableServer upstream = HttpServer.create().port(0).handle((request, response) -> {
        response.header("X-Upstream-Path", request.uri());
        String origin = request.requestHeaders().get("Origin");
        if (origin != null) {
            response.header("Access-Control-Allow-Origin", origin);
        }
        if (request.requestHeaders().contains("Access-Control-Request-Method")) {
            response.header("Access-Control-Allow-Methods", request.requestHeaders().get("Access-Control-Request-Method"));
        }
        if (request.uri().equals(TCX) && request.method().name().equals("POST")) {
            if (!"fixture".equals(request.requestHeaders().get("X-Health-Token"))) {
                return response.status(401).send();
            }
            response.header("X-Upstream-Auth", request.requestHeaders().get("Authorization", "absent"));
        }
        return response.send(request.receive().retain());
    }).bindNow();

    @DynamicPropertySource
    static void upstreamAddress(DynamicPropertyRegistry properties) {
        properties.add("HEALTH_INGEST_TOKEN", () -> "fixture");
        properties.add("BACKEND_BASE_URL", () -> "http://localhost:" + upstream.port());
    }

    @AfterAll
    static void stopUpstream() {
        upstream.disposeNow();
    }

    @LocalServerPort
    int port;

    @Test
    void forwardsHealthAndPreservesBackendAuthorization() {
        WebTestClient client = client();
        client.get().uri("/tymb/actuator/health").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Upstream-Path", "/tymb/actuator/health");
        client.post().uri(TCX).exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist("X-Upstream-Path");
        client.get().uri(TCX).exchange().expectStatus().isEqualTo(405);
        client.get().uri("/tymb/health-data/daily-lines").exchange().expectStatus().isEqualTo(405);
        client.post().uri("/tymb/health-data/daily-lines").exchange().expectStatus().isUnauthorized()
                .expectHeader().doesNotExist("X-Upstream-Path");
        client.post().uri("/tymb/health-data/daily-lines").header("X-Health-Token", "fixture")
                .exchange().expectStatus().isOk()
                .expectHeader().valueEquals("X-Upstream-Path", "/tymb/health-data/daily-lines");
    }

    @Test
    void streamsLargeTcxAndLeavesCorsAndAuthenticationToBackend() {
        byte[] payload = new byte[1024 * 1024 + 8192];
        new java.util.Random(7).nextBytes(payload);
        client().post().uri(TCX)
                .header("Origin", "https://tymultiverse.cc")
                .header("X-Health-Token", "fixture")
                .headers(headers -> headers.setBearerAuth("opaque-fixture"))
                .bodyValue(payload).exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Upstream-Auth", "Bearer opaque-fixture")
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "https://tymultiverse.cc")
                .expectBody().consumeWith(result -> assertThat(result.getResponseBody()).isEqualTo(payload));
        client().options().uri(TCX)
                .header("Origin", "https://tymultiverse.cc")
                .header("Access-Control-Request-Method", "POST")
                .exchange().expectStatus().isOk()
                .expectHeader().valueEquals("X-Upstream-Path", TCX)
                .expectHeader().valueEquals("Access-Control-Allow-Methods", "POST")
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "https://tymultiverse.cc");
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)).build();
    }
}
