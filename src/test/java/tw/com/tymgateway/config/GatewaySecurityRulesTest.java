package tw.com.tymgateway.config;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authorization matrix of the gateway's SecurityConfig. A probe controller answers every /tymg/** path with 200,
 * so the status is decided by the security rules alone (no backend, no Keycloak).
 */
@WebFluxTest(controllers = GatewaySecurityRulesTest.Probe.class)
@ContextConfiguration(classes = { GatewaySecurityRulesTest.Probe.class, SecurityConfig.class })
@TestPropertySource(properties = {
        "keycloak.auth-server-url=http://localhost:1/auth",
        "keycloak.realm=test"
})
class GatewaySecurityRulesTest {

    @RestController
    static class Probe {
        @RequestMapping("/**")
        String ok() {
            return "ok";
        }
    }

    @Autowired
    private WebTestClient client;

    private WebTestClient.ResponseSpec call(String method, String path, boolean withJwt) {
        WebTestClient c = withJwt ? client.mutateWith(mockJwt()) : client;
        return c.method(HttpMethod.valueOf(method)).uri(path)
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange();
    }

    /** Mutating gateway endpoints: must require a valid JWT at the edge (defence in depth; backend checks too). */
    static Stream<Arguments> writeEndpoints() {
        return Stream.of(
                Arguments.of("POST", "/tymg/people/insert"), Arguments.of("POST", "/tymg/people/update"),
                Arguments.of("POST", "/tymg/people/insert-multiple"), Arguments.of("POST", "/tymg/people/delete-all"),
                Arguments.of("POST", "/tymg/weapons"), Arguments.of("POST", "/tymg/weapons/insert-multiple"),
                Arguments.of("PUT", "/tymg/weapons/Sword/attributes"), Arguments.of("DELETE", "/tymg/weapons/Sword"),
                Arguments.of("DELETE", "/tymg/weapons/delete-all"),
                Arguments.of("POST", "/tymg/gallery/save"), Arguments.of("POST", "/tymg/gallery/update"),
                Arguments.of("POST", "/tymg/gallery/delete"), Arguments.of("PUT", "/tymg/gallery/1"),
                Arguments.of("DELETE", "/tymg/gallery/1"),
                Arguments.of("POST", "/tymg/ckeditor/save-content"),
                Arguments.of("POST", "/tymg/people-images"), Arguments.of("DELETE", "/tymg/people-images/bob"));
    }

    @ParameterizedTest(name = "anonymous {0} {1} -> 401")
    @MethodSource("writeEndpoints")
    void writeEndpoint_Should_Return401_When_Anonymous(String method, String path) {
        call(method, path, false).expectStatus().isUnauthorized();
    }

    @ParameterizedTest(name = "jwt {0} {1} -> 200")
    @MethodSource("writeEndpoints")
    void writeEndpoint_Should_PassThrough_When_Authenticated(String method, String path) {
        call(method, path, true).expectStatus().isOk();
    }

    /** Reads (including the POST-based queries the frontend uses) stay public. */
    static Stream<Arguments> publicEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/tymg/people/names"), Arguments.of("GET", "/tymg/people/damageWithWeapon?name=x"),
                Arguments.of("POST", "/tymg/people/get-all"), Arguments.of("POST", "/tymg/people/get-by-name"),
                Arguments.of("POST", "/tymg/people/batchDamageWithWeapon"),
                Arguments.of("GET", "/tymg/weapons"), Arguments.of("GET", "/tymg/weapons/Sword"),
                Arguments.of("POST", "/tymg/gallery/getAll"), Arguments.of("POST", "/tymg/gallery/getById"),
                Arguments.of("GET", "/tymg/gallery/1"),
                Arguments.of("POST", "/tymg/ckeditor/get-content"), Arguments.of("GET", "/tymg/ckeditor/x"),
                Arguments.of("GET", "/tymg/health/consumer"), Arguments.of("GET", "/tymg/actuator/health"),
                Arguments.of("GET", "/tymg/keycloak/redirect"), Arguments.of("POST", "/tymg/keycloak/introspect"),
                Arguments.of("GET", "/tymg/auth/visitor"), Arguments.of("GET", "/tymg/swagger-ui/index.html"),
                Arguments.of("OPTIONS", "/tymg/people/insert"));
    }

    @ParameterizedTest(name = "anonymous {0} {1} -> 200")
    @MethodSource("publicEndpoints")
    void publicEndpoint_Should_NotRequireCredentials(String method, String path) {
        call(method, path, false).expectStatus().isOk();
    }

    /** Everything else keeps requiring a login (rules that already worked). */
    static Stream<Arguments> loginRequiredEndpoints() {
        return Stream.of(Arguments.of("GET", "/tymg/learn/topics"), Arguments.of("POST", "/tymg/api/sync-characters"),
                Arguments.of("GET", "/tymg/people-images"), Arguments.of("GET", "/tymg/unknown"));
    }

    @ParameterizedTest(name = "anonymous {0} {1} -> 401")
    @MethodSource("loginRequiredEndpoints")
    void loginRequiredEndpoint_Should_Return401_When_Anonymous(String method, String path) {
        call(method, path, false).expectStatus().isUnauthorized();
    }
}
