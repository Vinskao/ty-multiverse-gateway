package tw.com.tymgateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import tw.com.tymgateway.dto.AsyncResultMessage;
import tw.com.tymgateway.dto.People;
import tw.com.tymgateway.service.AsyncResultRegistry;

/**
 * Gateway proxy error responses must not echo internal exception messages or async error details
 * (connection strings, SQL, credentials) to the caller; the details belong in the (masked) logs.
 */
class ProxyErrorResponseTest {

    private static final String SECRET_DETAIL = "insert failed jdbc:postgresql://admin:hunter2@postgres-0/people";

    private final AsyncResultRegistry registry = mock(AsyncResultRegistry.class);
    private final ObjectMapper json = new ObjectMapper();

    private static WebClient backendReturning(ExchangeFunction exchange) {
        return WebClient.builder().baseUrl("http://backend").exchangeFunction(exchange).build();
    }

    private static WebClient backendAccepting(String requestId) {
        return backendReturning(req -> Mono.just(ClientResponse.create(HttpStatus.ACCEPTED)
                .header("Content-Type", "application/json")
                .body("{\"success\":true,\"code\":202,\"message\":\"accepted\",\"requestId\":\"" + requestId + "\"}")
                .build()));
    }

    private String body(ResponseEntity<Object> response) throws Exception {
        return json.writeValueAsString(response.getBody());
    }

    @Test
    void failedAsyncResult_Should_NotEchoConsumerErrorDetail() throws Exception {
        AsyncPeopleProxyController controller = new AsyncPeopleProxyController(backendAccepting("r1"), registry, 5);
        AsyncResultMessage failed = new AsyncResultMessage();
        failed.setRequestId("r1");
        failed.setStatus("failed");
        failed.setError(SECRET_DETAIL);
        when(registry.awaitResult(eq("r1"), any(Duration.class))).thenReturn(Mono.just(failed));

        ResponseEntity<Object> response = controller.insertPerson(new People(), "Bearer t").block();

        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(body(response)).doesNotContain("hunter2").doesNotContain("jdbc").doesNotContain("insert failed");
    }

    @Test
    void asyncTimeout_Should_NotEchoThrowableMessage() throws Exception {
        AsyncPeopleProxyController controller = new AsyncPeopleProxyController(backendAccepting("r2"), registry, 5);
        when(registry.awaitResult(eq("r2"), any(Duration.class)))
                .thenReturn(Mono.error(new IllegalStateException(SECRET_DETAIL)));

        ResponseEntity<Object> response = controller.insertPerson(new People(), "Bearer t").block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(body(response)).doesNotContain("hunter2").doesNotContain("jdbc");
    }

    @Test
    void damageCallFailure_Should_NotEchoThrowableMessage() throws Exception {
        AsyncPeopleProxyController controller = new AsyncPeopleProxyController(
                backendReturning(req -> Mono.error(new IllegalStateException(SECRET_DETAIL))), registry, 5);

        ResponseEntity<Object> response = controller.calculateDamageWithWeapon("Bob", null).block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body(response)).doesNotContain("hunter2").doesNotContain("jdbc");
    }
}
