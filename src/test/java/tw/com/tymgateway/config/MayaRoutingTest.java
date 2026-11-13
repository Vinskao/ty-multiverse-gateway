package tw.com.tymgateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.DisposableServer;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.AfterAll;
import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MayaRoutingTest {
    static final DisposableServer upstream = HttpServer.create().port(0).handle((req, res) -> {
        res.header("Access-Control-Allow-Origin", "https://tymultiverse.cc");
        res.header("X-Upstream-Path", req.uri());
        res.header("X-Upstream-Auth", req.requestHeaders().get("Authorization", "absent"));
        res.header("X-Upstream-Xff", req.requestHeaders().get("X-Forwarded-For", "absent"));
        return res.send(req.receive().retain());
    }).bindNow();
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("MAYA_SAWA_BASE_URL", () -> "http://localhost:" + upstream.port());
    }
    @LocalServerPort int port;
    @AfterAll static void stop() { upstream.disposeNow(); }
    @Test void preservesPathsCredentialsCorsAndLargeStreamingBody() {
        WebTestClient client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)).responseTimeout(Duration.ofSeconds(20)).build();
        byte[] payload = new byte[1024 * 1024];
        for (String prefix : new String[]{"maya-sawa", "maya-v2", "voyeur", "paprika"}) {
            client.post().uri("/" + prefix + "/probe?q=1")
                    .header("Authorization", "Bearer maya-agent-key")
                    .header("Origin", "https://tymultiverse.cc")
                    .header("X-Forwarded-For", "203.0.113.7")
                    .bodyValue(payload).exchange().expectStatus().isOk()
                    .expectHeader().valueEquals("X-Upstream-Path", "/" + prefix + "/probe?q=1")
                    .expectHeader().valueEquals("X-Upstream-Auth", "Bearer maya-agent-key")
                    .expectHeader().value("X-Upstream-Xff", value -> org.assertj.core.api.Assertions.assertThat(value).contains("203.0.113.7"))
                    .expectHeader().valueEquals("Access-Control-Allow-Origin", "https://tymultiverse.cc")
                    .expectBody().consumeWith(result -> org.assertj.core.api.Assertions.assertThat(result.getResponseBody()).isEqualTo(payload));
        }
    }
}
