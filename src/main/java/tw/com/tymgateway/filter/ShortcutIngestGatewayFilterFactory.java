package tw.com.tymgateway.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import tw.com.tymgateway.config.GatewayConfig;

@Component
public class ShortcutIngestGatewayFilterFactory
        extends AbstractGatewayFilterFactory<ShortcutIngestGatewayFilterFactory.Config> {
    private static final String TOKEN_HEADER = "X-Health-Token";
    private static final long WINDOW_NANOS = Duration.ofMinutes(1).toNanos();
    private static final long SECOND_NANOS = Duration.ofSeconds(1).toNanos();

    private final byte[] expectedToken;
    private final int requestsPerMinute;
    private final LongSupplier time;

    @Autowired
    public ShortcutIngestGatewayFilterFactory(@Value("${HEALTH_INGEST_TOKEN:}") String token,
            @Value("${health-shortcut.requests-per-minute:12}") int requestsPerMinute) {
        this(token, requestsPerMinute, System::nanoTime);
    }

    ShortcutIngestGatewayFilterFactory(String token, int requestsPerMinute, LongSupplier time) {
        super(Config.class);
        if (requestsPerMinute < 1) {
            throw new IllegalArgumentException("health-shortcut.requests-per-minute must be positive");
        }
        this.expectedToken = token.isBlank() ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        this.requestsPerMinute = requestsPerMinute;
        this.time = time;
    }

    @Override
    public GatewayFilter apply(Config config) {
        RouteQuota quota = new RouteQuota();
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            var response = exchange.getResponse();
            if (HttpMethod.OPTIONS.equals(request.getMethod())
                    && exchange.getAttribute(GatewayConfig.BACKEND_PREFLIGHT_ORIGIN) != null
                    && HttpMethod.POST.equals(request.getHeaders().getAccessControlRequestMethod())) {
                return chain.filter(exchange);
            }
            if (!HttpMethod.POST.equals(request.getMethod())) {
                response.getHeaders().setAllow(java.util.Set.of(HttpMethod.POST));
                response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
                return response.setComplete();
            }
            if (expectedToken.length == 0) {
                response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                return response.setComplete();
            }
            List<String> supplied = request.getHeaders().getOrEmpty(TOKEN_HEADER);
            if (supplied.size() != 1
                    || !MessageDigest.isEqual(expectedToken, supplied.getFirst().getBytes(StandardCharsets.UTF_8))) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
            long retryAfter = quota.retryAfterSeconds();
            if (retryAfter > 0) {
                response.getHeaders().set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return response.setComplete();
            }
            return chain.filter(exchange);
        };
    }

    public static class Config {
    }

    private final class RouteQuota {
        private long windowStarted;
        private int used;

        synchronized long retryAfterSeconds() {
            long now = time.getAsLong();
            long elapsed = now - windowStarted;
            if (used == 0 || elapsed >= WINDOW_NANOS) {
                windowStarted = now;
                used = 1;
                return 0;
            }
            if (used < requestsPerMinute) {
                used++;
                return 0;
            }
            return Math.ceilDiv(WINDOW_NANOS - elapsed, SECOND_NANOS);
        }
    }
}
