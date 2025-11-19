# Gateway 路由策略文档

## 🎯 **路由架构概览**

TY Multiverse Gateway 采用**混合路由架构**，结合 Spring Cloud Gateway 和手动控制器的优势：

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend Request                          │
│                  (http://localhost:8082/tymg/*)             │
│                          ↓                                   │
│              ┌───────────┴───────────┐                      │
│              ↓                       ↓                       │
│   Spring Cloud Gateway        Manual Controllers            │
│   (Simple HTTP Routes)        (gRPC + Complex Logic)        │
└─────────────────────────────────────────────────────────────┘
```

## 📊 **路由分工**

### 1. Spring Cloud Gateway 路由 (application.yml)

**处理场景**: 简单的 HTTP 请求转发，无需复杂逻辑

**路由列表**:
- `/api/request-status/**` - 异步请求状态查询
- `/api/people/result/**` - People 异步结果查询
- `/api/test/async/**` - 异步测试端点
- `/auth/**` - 认证相关端点
- `/health/**` - 健康检查
- `/actuator/**` - Spring Boot Actuator
- `/swagger-ui/**`, `/v3/api-docs/**` - API 文档
- `/docs/**` - JavaDoc
- `/people-images/**` - 图片资源

**优势**:
- ✅ 配置驱动，无需代码
- ✅ 内置负载均衡、重试、断路器
- ✅ 易于维护和修改

**配置示例**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: async-request-status-route
          uri: http://localhost:8080
          predicates:
            - Path=/api/request-status/**
            - Method=GET,DELETE
          filters:
            - StripPrefix=1
```

### 2. 专用 gRPC Controllers

**处理场景**: 需要 gRPC 客户端调用 Backend gRPC 服务的复杂路由

**控制器列表**:
- `PeopleController` - `/people/**` (gRPC: PeopleService)
- `WeaponController` - `/weapons/**` (gRPC: WeaponService)
- `GalleryController` - `/gallery/**` (gRPC: GalleryService)
- `DeckofcardsController` - `/deckofcards/**` (gRPC: DeckofcardsService)

**优势**:
- ✅ 精确控制 gRPC 调用
- ✅ 支持异步模式 (Producer-Consumer)
- ✅ 可以添加自定义业务逻辑

**代码示例**:
```java
@RestController
@RequestMapping("/people")
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class PeopleController {
    @Autowired
    private PeopleGrpcClient grpcClient;
    
    @PostMapping("/get-all")
    public Mono<ProducerResponse> getAllPeople() {
        return grpcClient.getAllPeople();
    }
}
```

### 3. 通用路由控制器 (GatewayRouterController)

**处理场景**: Fallback，处理未被上述两者匹配的请求

**路由**: `/**` (通配符，最低优先级)

**优势**:
- ✅ 作为安全网，防止请求丢失
- ✅ 支持所有 HTTP 方法 (GET, POST, PUT, DELETE)
- ✅ 灵活的错误处理

## 🔢 **路由优先级**

Spring MVC/WebFlux 的路由匹配优先级（从高到低）：

1. **精确路径匹配** - `@RequestMapping("/people/get-all")`
2. **路径变量匹配** - `@RequestMapping("/people/{id}")`
3. **路径模式匹配** - `@RequestMapping("/people/*")`
4. **Spring Cloud Gateway 路由** - `Path=/api/**`
5. **通配符匹配** - `@RequestMapping("/**")`

### 实际路由流程

```
请求: GET /tymg/people/get-all
  ↓
1. 检查 PeopleController (@RequestMapping("/people"))
   → 匹配! 使用 gRPC 调用
   
请求: GET /tymg/api/request-status/123
  ↓
1. 检查专用 Controllers
   → 不匹配
2. 检查 Spring Cloud Gateway routes
   → 匹配! 转发到 Backend
   
请求: GET /tymg/unknown/path
  ↓
1. 检查专用 Controllers
   → 不匹配
2. 检查 Spring Cloud Gateway routes
   → 不匹配
3. 使用 GatewayRouterController fallback
   → 转发到 Backend (可能返回 404)
```

## 🔧 **配置要点**

### 1. Context Path

```yaml
server:
  servlet:
    context-path: /tymg
```

所有请求必须以 `/tymg` 开头。

### 2. gRPC 客户端启用

```yaml
grpc:
  client:
    enabled: true
```

控制是否启用 gRPC 客户端和专用控制器。

### 3. Backend 服务 URL

```yaml
PUBLIC_TYMB_URL: http://localhost:8080
```

Spring Cloud Gateway 和 WebClient 使用此 URL 转发请求。

## 📋 **端点映射表**

| Frontend Endpoint | 处理方式 | 转发目标 | 说明 |
|-------------------|---------|---------|------|
| `/tymg/people/get-all` | gRPC Controller | gRPC: PeopleService | 异步模式 |
| `/tymg/weapons` | gRPC Controller | gRPC: WeaponService | 同步模式 |
| `/tymg/gallery/getAll` | gRPC Controller | gRPC: GalleryService | 同步模式 |
| `/tymg/api/request-status/*` | Spring Cloud Gateway | Backend REST API | HTTP 转发 |
| `/tymg/api/people/result/*` | Spring Cloud Gateway | Backend REST API | HTTP 转发 |
| `/tymg/auth/*` | Spring Cloud Gateway | Backend REST API | HTTP 转发 |
| `/tymg/health` | Spring Cloud Gateway | Backend REST API | HTTP 转发 |
| `/tymg/actuator/*` | Spring Cloud Gateway | Backend Actuator | HTTP 转发 |

## 🚀 **最佳实践**

### 1. 添加新的简单 HTTP 端点

在 `application.yml` 中添加路由：

```yaml
routes:
  - id: new-route
    uri: '@PUBLIC_TYMB_URL@'
    predicates:
      - Path=/api/new-endpoint/**
      - Method=GET,POST
    filters:
      - StripPrefix=1
```

### 2. 添加新的 gRPC 端点

创建专用控制器：

```java
@RestController
@RequestMapping("/new-module")
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class NewModuleController {
    @Autowired
    private NewModuleGrpcClient grpcClient;
    
    @GetMapping("/data")
    public Mono<ResponseEntity<String>> getData() {
        return grpcClient.getData();
    }
}
```

### 3. 调试路由问题

1. 检查日志中的请求路径
2. 确认路由优先级
3. 使用 Actuator 查看路由配置:
   ```bash
   curl http://localhost:8082/tymg/actuator/gateway/routes
   ```

## ⚠️ **常见问题**

### Q1: 为什么有些请求返回 404？

**A**: 检查路由优先级。可能被 `/**` 通配符拦截但 Backend 没有对应端点。

### Q2: Spring Cloud Gateway 路由不生效？

**A**: 确保没有被专用控制器的 `@RequestMapping` 优先匹配。

### Q3: gRPC 调用失败？

**A**: 检查 `grpc.client.enabled=true` 和 Backend gRPC 服务是否启动。

## 📝 **维护建议**

1. **定期审查路由配置** - 避免重复或冲突的路由
2. **文档同步** - 更新路由时同步更新此文档
3. **监控路由性能** - 使用 Actuator metrics 监控
4. **测试覆盖** - 为每个路由编写集成测试

---

**最后更新**: 2025-11-07  
**版本**: 2.0  
**维护者**: TY Team

