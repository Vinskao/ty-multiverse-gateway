# TY Multiverse Gateway - Agent Guide

## 📁 文档组织规定

**重要**：所有非 `AGENTS.md` 和 `README.md` 的 Markdown 文档都必须放在项目的 `/docs` 目录下。

- ✅ **允许在根目录**：`AGENTS.md`、`README.md`
- ✅ **必须放在 `/docs`**：所有其他 `.md` 文件（如 `JENKINS_CREDENTIALS.md`、`ARCHITECTURE.md` 等）
- 📂 **文档目录结构**：`/docs/` 目录下可以创建子目录来组织相关文档

## Project Overview

TY Multiverse Gateway is a Spring Cloud Gateway application that serves as the unified API entry point for the TY Multiverse system. It provides centralized authentication, rate limiting, circuit breaking, and request routing to backend services.

### Architecture
- **Framework**: Spring Cloud Gateway with Spring Boot 3.2.7
- **Protocol**: HTTP/HTTPS with WebFlux reactive stack
- **Security**: JWT token validation and Keycloak integration
- **Resilience**: Circuit breaker and rate limiting patterns
- **Monitoring**: Comprehensive request/response logging

### Key Components
- **Route Management**: Dynamic request routing to backend services
- **Authentication Gateway**: JWT token validation and refresh
- **Rate Limiting**: Redis-based distributed rate limiting
- **Circuit Breaker**: Resilience4j circuit breaker implementation
- **CORS Handling**: Cross-origin resource sharing configuration
- **Metrics Collection**: Request metrics and performance monitoring

## Security Hardening (2026-06-19)

### #4 Keycloak Callback: Stop Logging Tokens (P4a Log Redaction)

**問題**：[KeycloakController.java](src/main/java/tw/com/tymgateway/controller/KeycloakController.java) 在 Keycloak 回呼時
- 第 184–185 行明文輸出整個 access_token / refresh_token 到日誌
- 第 241–243 行輸出完整 redirect URL（含所有 token 明文）
- 日誌會被收集到中央日誌系統，形成長期機密洩漏

**修正** (KeycloakController.java)：
1. 第 184–186 行：不輸出 token 明文，改輸出長度與 "already obtained" 標記
   ```java
   log.info("Access Token: {}", accessToken != null ? "已取得(len=" + accessToken.length() + ")" : "null");
   ```

2. 第 237–243 行：移除輸出完整 redirect URL，改輸出只有 username 的提示
   ```java
   log.info("重定向目標(不含機密): {}?username=...", frontendUrl);
   ```

**部署注意**：
- 此改動僅影響日誌輸出，不改功能邏輯
- Build & deploy 照常（無行為改變）
- 日誌審計：檢查舊日誌是否已歸檔、刪除或 redact（不在本改動範圍，需 DevOps/InfoSec 跟進）

**後續** (P4b 待做)：回呼的 redirect URL 本身仍會把 token 帶過去（給前端 JS）。前端 P2 已在落地後立刻 replaceState 洗網址；P4b 計畫改用 httpOnly cookie。

---

## Build and Test Commands

### Prerequisites

⚠️ **重要：依賴版本更新**

**必須確保 `ty-multiverse-common` 依賴版本更新到最新版本！**

在 `pom.xml` 中檢查並更新：
```xml
<dependency>
    <groupId>tw.com.ty</groupId>
    <artifactId>ty-multiverse-common</artifactId>
    <version>2.2.2</version>  <!-- 請更新到最新版本 -->
</dependency>
```

**為什麼重要？**
- 舊版本可能缺少新的常數（如 `MessageKey.LOGOUT_SUCCESS`）
- 會導致編譯錯誤：`cannot find symbol`
- 新功能和修復只在最新版本中可用

**如何檢查最新版本？**
```bash
# 檢查 common 模組的當前版本
cd ../ty-multiverse-common
cat pom.xml | grep "<version>"

# 或在 GitHub Packages 查看最新發布版本
```

```bash
# Ensure Java 21 is installed
java -version

# Verify Maven installation
./mvnw --version

# Ensure backend service is running for integration
curl http://localhost:8080/actuator/health
```

### Build Commands
```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Package application
./mvnw package -DskipTests

# Create executable JAR
./mvnw package

# Build and run in one command
./mvnw clean package -DskipTests && java -jar target/ty-multiverse-gateway.jar
```

### Development Mode
```bash
# Start gateway service
./mvnw spring-boot:run

# Start with debug logging
./mvnw spring-boot:run -Dlogging.level.tw.com.tymgateway=DEBUG

# Start with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Test Commands
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=GatewayIntegrationTest

# Run with coverage
./mvnw test jacoco:report

# Performance tests
./mvnw test -Dtest=*Performance*
```

## Code Style Guidelines

### Java Code Style
- **Language Level**: Java 21
- **Formatting**: Standard Java conventions with 4-space indentation
- **Naming**: camelCase for methods/variables, PascalCase for classes
- **Line Length**: Max 120 characters
- **Reactive Programming**: Use WebFlux reactive patterns

### Gateway-Specific Conventions
```java
// ✅ Good: Proper reactive gateway filter
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .doOnEach(signal -> {
                    if (signal.hasValue()) {
                        log.info("Request processed successfully");
                    }
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

// ❌ Avoid: Blocking operations in reactive context
public class BadFilter implements GlobalFilter {
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Don't do this - blocking operation
        String result = someBlockingService.getData();
        return chain.filter(exchange);
    }
}
```

### Package Structure
```
src/main/java/tw/com/tymgateway/
├── config/           # Configuration classes
├── controller/       # REST controllers
├── filter/          # Gateway filters
├── grpc/            # gRPC client integrations
├── dto/             # Data transfer objects
└── TymGatewayApplication.java
```

## Testing Instructions

### Unit Tests
- **Focus**: Test individual filters and utilities
- **Framework**: JUnit 5 with Spring WebFlux Test
- **Mocking**: Use Mockito for external dependencies
- **Reactive Testing**: Use StepVerifier for reactive streams

### Integration Tests
- **Scope**: Test complete request flows through gateway
- **Setup**: Use @SpringBootTest with test containers
- **Verification**: Ensure proper routing and filtering

### Test Examples
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    void testPeopleRoute() {
        webClient.get()
                .uri("/tymg/people/get-all")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    void testRateLimiting() {
        // Send multiple requests rapidly
        for (int i = 0; i < 10; i++) {
            webClient.get().uri("/tymg/people/get-all").exchange();
        }

        // Should eventually get rate limited
        webClient.get()
                .uri("/tymg/people/get-all")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
```

## Security Considerations

### Gateway Security
- **Authentication**: JWT token validation for all routes
- **Authorization**: Role-based access control
- **Rate Limiting**: Prevent abuse and DoS attacks
- **CORS Policy**: Strict cross-origin policies

### Token Management
- **Token Validation**: Verify JWT signatures and claims
- **Token Refresh**: Handle token refresh flows
- **Token Revocation**: Handle logout and token invalidation
- **Secure Headers**: Implement security headers

### Network Security
- **HTTPS Enforcement**: All traffic must use HTTPS
- **Backend Security**: Secure communication with backend services
- **Input Validation**: Validate all incoming requests
- **Error Handling**: Avoid information leakage in error responses

## Additional Instructions

### Commit Message Guidelines
```bash
# Format: <type>(<scope>): <description>

feat(routes): add new route configuration for weapons service
fix(filters): resolve circuit breaker configuration issue
docs(readme): update gateway architecture documentation
test(routing): add integration tests for route matching
refactor(config): improve gateway configuration structure
```

### Pull Request Process
1. **Branch Naming**: `feature/`, `fix/`, `refactor/` prefixes
2. **Small Changes**: Keep PRs focused on single features/fixes
3. **Testing**: Ensure all tests pass and add new tests for features
4. **Documentation**: Update README and configuration docs

### Deployment Steps

#### Development Deployment
```bash
# 1. Start backend service first
cd ../ty-multiverse-backend
./mvnw spring-boot:run

# 2. Start gateway in another terminal
cd ../ty-multiverse-gateway
./mvnw spring-boot:run

# 3. Test gateway routes
curl http://localhost:8082/actuator/gateway/routes
```

#### Production Deployment
```bash
# 1. Build optimized JAR
./mvnw clean package -Pprod -DskipTests

# 2. Deploy with proper configuration
java -jar target/ty-multiverse-gateway.jar \
  --spring.profiles.active=prod \
  --server.port=8082 \
  --PUBLIC_TYMB_URL=http://backend:8080
```

### Route Configuration
```yaml
# application.yml route examples
routes:
  - id: people-route
    uri: http://backend:8080
    predicates:
      - Path=/tymg/people/**
      - Method=GET,POST,PUT,DELETE
    filters:
      - StripPrefix=1
      - CircuitBreaker=backendCircuitBreaker
      - RequestRateLimiter=rateLimiter
```

### Monitoring and Observability
- **Health Checks**: Gateway and backend health monitoring
- **Metrics**: Request count, response times, error rates
- **Distributed Tracing**: Request correlation across services
- **Log Aggregation**: Centralized logging configuration

### Performance Optimization
- **Connection Pooling**: Optimize HTTP client connection pools
- **Timeout Configuration**: Proper request and response timeouts
- **Load Balancing**: Configure load balancing strategies
- **Caching**: Implement response caching where appropriate

### Circuit Breaker Configuration
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 100
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
```

### Troubleshooting
- **Route Issues**: Check route predicates and filters
- **Backend Connectivity**: Verify backend service availability
- **Authentication Issues**: Check JWT token validation
- **Performance Issues**: Monitor response times and throughput

### Environment Variables
```bash
# Service URLs
PUBLIC_TYMB_URL=http://localhost:8080
FRONTEND_URL=http://localhost:4321

# Redis (for rate limiting)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Keycloak (for token validation)
KEYCLOAK_URL=https://your-keycloak.com
KEYCLOAK_REALM=your-realm

# Gateway Configuration
GATEWAY_PORT=8082
LOGGING_LEVEL_TW_COM_TYMGATEWAY=INFO
```

### Development Workflow
1. **Configuration**: Update route configurations for new endpoints
2. **Testing**: Test route matching and filtering
3. **Integration**: Verify end-to-end request flows
4. **Monitoring**: Set up proper monitoring and alerting
5. **Deployment**: Deploy with proper configuration management

### IDE Setup
```bash
# VS Code extensions recommended
# - Spring Boot Extension Pack
# - Java Language Support
# - Maven for Java

# IntelliJ IDEA
# - Spring Boot plugin
# - Java 21 support
# - Maven integration
```

---

## 啟動順序（必須先 Backend）

```bash
# 1. 先啟動 Backend
cd ../ty-multiverse-backend
mvn spring-boot:run

# 2. 再啟動 Gateway
cd ../ty-multiverse-gateway
./mvnw clean generate-sources compile test-compile
mvn spring-boot:run
```

服務端點：`http://localhost:8082`

## API 測試指令

```bash
# 健康檢查
curl http://localhost:8082/actuator/health
curl http://localhost:8082/tymg/actuator/health
curl http://localhost:8082/actuator/gateway/routes

# People API
curl -X GET "http://localhost:8082/tymg/api/people/names"
curl -X POST "http://localhost:8082/tymg/api/people/get-by-name" \
  -H "Content-Type: application/json" -d '{"name": "Maya"}'
curl -X POST "http://localhost:8082/tymg/api/people/get-all"

# Weapons API
curl -X GET "http://localhost:8082/tymg/api/people/weapons"
curl -X GET "http://localhost:8082/tymg/api/people/damage?name=Maya"

# 監控指標
curl http://localhost:8082/actuator/metrics
curl http://localhost:8082/actuator/prometheus

# 非同步 Consumer 測試
curl "http://localhost:8082/tymg/api/request-status?requestId=your-request-id"
curl -X POST "http://localhost:8082/tymg/api/test/async/damage-calculation" \
  -H "Content-Type: application/json" -d '{"requestId": "uuid-123", "characterName": "Maya"}'
```

## Docker 部署

```bash
docker build -t ty-multiverse-gateway:latest .
docker run -p 8081:8081 \
  -e PUBLIC_TYMB_URL=http://backend:8080 \
  -e PUBLIC_FRONTEND_URL=http://your-frontend-url \
  ty-multiverse-gateway:latest
```

## Kubernetes 部署

```bash
cd k8s
kubectl apply -f deployment.yaml -n ty-multiverse
kubectl get pods -n ty-multiverse -l app=ty-multiverse-gateway
kubectl logs -f -n ty-multiverse -l app=ty-multiverse-gateway
kubectl get svc -n ty-multiverse ty-multiverse-gateway-service
```

## 性能調優配置

限流（`application.yml`）：
```yaml
redis-rate-limiter.replenishRate: 100  # 每秒補充令牌數
redis-rate-limiter.burstCapacity: 200  # 令牌桶容量
```

熔斷器：
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 100
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

本地 `local.properties` 配置：
```properties
PUBLIC_TYMB_URL=http://localhost:8080
PUBLIC_FRONTEND_URL=http://localhost:4321
REDIS_HOST=localhost
REDIS_CUSTOM_PORT=6379
REDIS_PASSWORD=
```
