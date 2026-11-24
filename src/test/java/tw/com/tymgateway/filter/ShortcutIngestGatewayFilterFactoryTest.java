package tw.com.tymgateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;
import tw.com.tymgateway.config.GatewayConfig;

class ShortcutIngestGatewayFilterFactoryTest {
    private static final String TCX = "/tymb/health-data/workout-tcx";
    private final AtomicLong time = new AtomicLong();
    private final GatewayFilter filter = new ShortcutIngestGatewayFilterFactory("fixture", 2, time::get)
            .apply(new ShortcutIngestGatewayFilterFactory.Config());
    private final GatewayFilterChain upstream = mock(GatewayFilterChain.class);

    @Test
    void rejectsOtherMethodsAndMissingOrInvalidTokensBeforeRouting() {
        assertThat(run(MockServerHttpRequest.get(TCX))).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(run(MockServerHttpRequest.post(TCX))).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(run(MockServerHttpRequest.post(TCX).header("X-Health-Token", "wrong")))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(run(MockServerHttpRequest.post(TCX).header("X-Health-Token", "fixture", "fixture")))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(upstream);
    }

    @Test
    void onlyAuthenticatedPostsUseQuotaAndItResetsAfterOneMinute() {
        when(upstream.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
        assertThat(run(MockServerHttpRequest.post(TCX))).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(run(MockServerHttpRequest.post(TCX).header("X-Health-Token", "fixture"))).isNull();
        assertThat(run(MockServerHttpRequest.post(TCX).header("X-Health-Token", "fixture"))).isNull();

        var limited = MockServerWebExchange.from(
                MockServerHttpRequest.post(TCX).header("X-Health-Token", "fixture").build());
        filter.filter(limited, upstream).block();
        assertThat(limited.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(limited.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");

        time.addAndGet(Duration.ofMinutes(1).toNanos());
        assertThat(run(MockServerHttpRequest.post(TCX).header("X-Health-Token", "fixture"))).isNull();
    }

    @Test
    void forwardsOnlyPostPreflightsWithoutTokenOrQuota() {
        when(upstream.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
        var preflight = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.OPTIONS, TCX)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST").build());
        preflight.getAttributes().put(GatewayConfig.BACKEND_PREFLIGHT_ORIGIN, "https://tymultiverse.cc");
        filter.filter(preflight, upstream).block();
        assertThat(preflight.getResponse().getStatusCode()).isNull();
        assertThat(run(MockServerHttpRequest.method(HttpMethod.OPTIONS, TCX)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    private HttpStatus run(MockServerHttpRequest.BaseBuilder<?> request) {
        var exchange = MockServerWebExchange.from(request.build());
        filter.filter(exchange, upstream).block();
        return (HttpStatus) exchange.getResponse().getStatusCode();
    }
}
