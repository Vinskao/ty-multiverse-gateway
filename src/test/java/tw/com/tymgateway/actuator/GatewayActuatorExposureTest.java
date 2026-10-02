package tw.com.tymgateway.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * The gateway's own actuator is reachable from every pod in the cluster (no NetworkPolicy), so it must not
 * offer the route-management endpoint (POST /actuator/gateway/routes/** lets a caller add proxy routes) nor
 * infrastructure details in /actuator/health. Runs the full application with the production profile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("platform")
class GatewayActuatorExposureTest {

    @Autowired
    private WebTestClient client;

    @Test
    void routeManagementEndpoint_Should_NotBeExposed() {
        client.get().uri("/actuator/gateway/routes").exchange().expectStatus().isNotFound();
        client.post().uri("/actuator/gateway/refresh").exchange().expectStatus().isNotFound();
    }

    @Test
    void health_Should_BePublicButWithoutComponents() {
        client.get().uri("/actuator/health").exchange()
                .expectBody().jsonPath("$.status").exists().jsonPath("$.components").doesNotExist();
    }
}
