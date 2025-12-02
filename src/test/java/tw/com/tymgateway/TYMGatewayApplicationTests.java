package tw.com.tymgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Gateway 應用測試類
 */
@SpringBootTest
@ActiveProfiles("test")
class TYMGatewayApplicationTests {

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void contextLoads() {
        // 測試應用上下文加載
    }
}

