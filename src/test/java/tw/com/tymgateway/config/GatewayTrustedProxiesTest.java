package tw.com.tymgateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.gateway.filter.headers.TrustedProxies;
import org.springframework.core.io.ClassPathResource;

/**
 * Since Spring Cloud Gateway 4.3 (CVE-2025-41235) the X-Forwarded-For chain only keeps entries matching
 * spring.cloud.gateway.server.webflux.trusted-proxies; without the property no X-Forwarded-* header is sent.
 * maya-sawa rate-limits anonymous callers per client IP and resolves that IP itself (walking the chain from the
 * right against its own trusted-proxy CIDRs), so the gateway must pass the public client hop through unchanged.
 * End-to-end coverage: {@link MayaRoutingTest#preservesPathsCredentialsCorsAndLargeStreamingBody()}.
 */
class GatewayTrustedProxiesTest {

    @Test
    void forwardedChain_Should_KeepPrivateProxiesAndPublicClientHops() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        String pattern = yaml.getObject().getProperty("spring.cloud.gateway.server.webflux.trusted-proxies");
        assertThat(pattern).as("spring.cloud.gateway.server.webflux.trusted-proxies").isNotBlank();

        TrustedProxies trusted = TrustedProxies.from(pattern);
        for (String address : new String[]{"127.0.0.1", "10.244.1.5", "192.168.1.20", "0:0:0:0:0:0:0:1",
                "203.0.113.7", "8.8.8.8"}) {
            assertThat(trusted.isTrusted(address)).as(address).isTrue();
        }
    }
}
