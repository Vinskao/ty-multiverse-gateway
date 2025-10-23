# TY Multiverse Gateway

**API Gateway for TY Multiverse system using Spring Cloud Gateway.**

TY Multiverse Gateway 是整個 TY Multiverse 系統的統一 API 入口點，負責將前端的所有請求路由到後端服務，並提供統一的安全、監控和流量控制功能。

## 🎯 Gateway 角色定位

Gateway 作為系統的**統一入口閘道**，專注於以下職責：

- **🔐 統一認證授權**：處理 JWT 驗證和權限檢查
- **🚦 流量控制**：實現限流、熔斷和負載均衡
- **📊 統一監控**：記錄所有請求響應和性能指標
- **🔄 協議轉換**：將 HTTP 請求轉換為 gRPC 調用
- **🛡️ 安全防護**：CORS、請求驗證和異常處理

## 🚀 快速開始

### 環境準備
1. **啟動 Backend 服務**（必須）
   ```bash
   cd ../ty-multiverse-backend
   mvn spring-boot:run
   ```

2. **啟動 Gateway**
   ```bash
   mvn spring-boot:run
   ```

### 服務端點
- **Gateway API**: `http://localhost:8082`
- **健康檢查**: `http://localhost:8082/actuator/health`
- **API 文檔**: `http://localhost:8082/tymgateway/tymb/people/docs`

### 測試指令
```bash
# 檢查健康狀態
curl http://localhost:8082/actuator/health

# 測試人物 API
curl http://localhost:8082/tymgateway/tymb/people/get-all

# 查看路由配置
curl http://localhost:8082/actuator/gateway/routes
```

## 🔧 開發環境設定

### 依賴關係說明

**重要：必須按順序啟動服務**

1. **Backend 服務** - 提供實際業務功能
   ```bash
   cd ../ty-multiverse-backend
   mvn spring-boot:run
   ```

2. **Gateway** - API 網關入口
   ```bash
   mvn spring-boot:run
   ```

**服務連接：**
- Backend HTTP: `localhost:8080`
- Backend gRPC: `localhost:50051`
- Gateway API: `localhost:8082`

## 🚀 Gateway 服務啟動

### 啟動指令
```bash
mvn spring-boot:run
```

### 服務端點
- **API 入口**: `http://localhost:8082`
- **健康檢查**: `http://localhost:8082/actuator/health`
- **路由資訊**: `http://localhost:8082/actuator/gateway/routes`
- **API 文檔**: `http://localhost:8082/tymgateway/tymb/people/docs`

### 測試指令
```bash
# 健康檢查
curl http://localhost:8082/actuator/health

# 測試人物 API
curl http://localhost:8082/tymgateway/tymb/people/get-all

# 查看路由配置
curl http://localhost:8082/actuator/gateway/routes
```

### 前置條件
**必須先啟動 Backend 服務：**
```bash
cd ../ty-multiverse-backend
mvn spring-boot:run
```

## 📊 監控與狀態檢查

### 監控端點
```bash
# Gateway 健康檢查
curl http://localhost:8082/actuator/health

# 路由配置檢查
curl http://localhost:8082/actuator/gateway/routes

# 性能指標
curl http://localhost:8082/actuator/metrics
```

### 啟動日誌確認
啟動時會看到：
```
🚀 初始化 gRPC Keycloak Client，連接後端: localhost:50051
✅ gRPC Keycloak Client 初始化完成（使用模擬實現）
🚀 初始化 gRPC People Client，連接後端: localhost:50051
✅ gRPC People Client 初始化完成（使用模擬實現）
```

## 🎯 Gateway 核心功能

TY Multiverse Gateway 作為微服務架構中的 API 網關，提供：

- **🔐 統一認證授權**：JWT 驗證和權限檢查
- **🚦 流量控制**：限流、熔斷和負載均衡
- **📊 統一監控**：請求響應記錄和性能指標
- **🔄 協議轉換**：HTTP 請求轉換為 gRPC 調用
- **🛡️ 安全防護**：CORS、請求驗證和異常處理

## 系統架構圖

### gRPC 三層架構

```
┌─────────────────────────────────────────────────────────────┐
│                    gRPC 三層架構                              │
├─────────────────────────────────────────────────────────────┤
│  Backend (ty-multiverse-backend)                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  GrpcServerConfig (服務器配置)                        │   │
│  │  ├─ 啟動 gRPC Server (端口 50051)                  │   │
│  │  ├─ 註冊 PeopleService                              │   │
│  │  └─ 註冊 KeycloakService                           │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  GrpcPeopleServiceImpl (People 服務實現)            │   │
│  │  ├─ getAllPeople()                                 │   │
│  │  ├─ getPeopleByName()                              │   │
│  │  └─ insertPeople()                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  GrpcKeycloakServiceImpl (Keycloak 服務實現)         │   │
│  │  ├─ processAuthRedirect()                           │   │
│  │  ├─ logout()                                        │   │
│  │  └─ introspectToken()                               │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                                │
                                │ gRPC 調用
                                ▼
┌─────────────────────────────────────────────────────────────┐
│  Gateway (ty-multiverse-gateway)                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  PeopleGrpcClient (People 客戶端)                   │   │
│  │  ├─ getAllPeople()                                 │   │
│  │  ├─ getPeopleByName()                              │   │
│  │  └─ insertPeople()                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  KeycloakGrpcClient (Keycloak 客戶端)               │   │
│  │  ├─ processAuthRedirect()                           │   │
│  │  ├─ logout()                                        │   │
│  │  └─ introspectToken()                               │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  HTTP Controllers (REST API 接口)                    │   │
│  │  ├─ PeopleController                               │   │
│  │  └─ KeycloakController                              │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 完整系統架構

```
┌─────────────┐
│   Frontend  │
│ (Astro/JS)  │
└──────┬──────┘
       │
       │ HTTP/HTTPS
       ▼
┌─────────────────────────────────────────────┐
│          TY Multiverse Gateway              │
│         (Spring Cloud Gateway)              │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │      Global Filters                  │  │
│  │  • Logging                           │  │
│  │  • CORS                              │  │
│  │  • Rate Limiting                     │  │
│  │  • Circuit Breaker                   │  │
│  └──────────────────────────────────────┘  │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │      Route Configurations            │  │
│  │  • /tymb/people/** → Backend         │  │
│  │  • /tymb/weapons/** → Backend        │  │
│  │  • /tymb/gallery/** → Backend        │  │
│  │  • ... (all backend routes)          │  │
│  └──────────────────────────────────────┘  │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │      gRPC Clients                    │  │
│  │  • PeopleGrpcClient                  │  │
│  │  • KeycloakGrpcClient                │  │
│  └──────────────────────────────────────┘  │
└──────────────┬──────────────────────────────┘
               │
               │ Load Balanced HTTP + gRPC
               ▼
┌─────────────────────────────────────────────┐
│      TY Multiverse Backend Service          │
│         (Spring Boot REST API)              │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │      gRPC Server                        │  │
│  │  • GrpcServerConfig                │  │
│  │  • GrpcPeopleServiceImpl            │  │
│  │  • GrpcKeycloakServiceImpl          │  │
│  └──────────────────────────────────────┘  │
│                                             │
│  • People Management                        │
│  • Weapons Management                       │
│  • Gallery Management                       │
│  • Authentication (Keycloak)                │
│  • Async Processing                         │
└─────────────────────────────────────────────┘
               │
               ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│PostgreSQL│  │  Redis   │  │RabbitMQ  │
└──────────┘  └──────────┘  └──────────┘
```

## 技術架構

- **Spring Boot**: 3.2.7
- **Spring Cloud Gateway**: 2023.0.2
- **Java**: 21
- **Redis**: 用於分散式限流
- **Resilience4j**: 熔斷器實現

## 核心組件

### 1. Route Predicates（路由斷言）

路由斷言定義了哪些請求應該被路由到哪個服務。

```yaml
routes:
  - id: people-route
    uri: http://backend:8080
    predicates:
      - Path=/tymb/people/**
```

### 2. Gateway Filters（網關過濾器）

#### 2.1 CircuitBreaker（熔斷器）

使用 Resilience4j 實現：
- 監控後端服務健康狀況
- 當失敗率超過閾值時自動熔斷
- 提供降級響應

```yaml
filters:
  - name: CircuitBreaker
    args:
      name: backendCircuitBreaker
      fallbackUri: forward:/fallback
```

#### 2.2 RequestRateLimiter（限流器）

基於 Redis 的分散式限流：
- 令牌桶算法
- 支援分散式部署
- 可配置補充速率和容量

### 3. Global Filters（全局過濾器）

#### 3.1 LoggingGlobalFilter

記錄所有請求和響應：
- 請求方法和路徑
- 來源 IP
- 響應狀態碼
- 處理時間

#### 3.2 CORS Filter

統一處理跨域請求：
- 允許特定來源
- 配置允許的方法和標頭
- 支援憑證傳遞

### 4. Fallback Controller（降級控制器）

當後端服務不可用時提供降級響應：
- 返回標準錯誤格式
- 503 Service Unavailable
- 包含時間戳和錯誤信息

## 請求流程

```
1. 客戶端發送請求
   ↓
2. Gateway 接收請求
   ↓
3. LoggingGlobalFilter 記錄請求
   ↓
4. CORS Filter 處理跨域
   ↓
5. RequestRateLimiter 檢查限流
   ↓
6. 路由匹配（Route Predicates）
   ↓
7. CircuitBreaker 檢查後端健康狀態
   ↓
8. 轉發請求到 Backend
   ↓
9. Backend 處理並返回響應
   ↓
10. Gateway 返回響應給客戶端
    ↓
11. LoggingGlobalFilter 記錄響應
```

## 性能特性

### 1. 反應式架構

基於 Spring WebFlux：
- 非阻塞 I/O
- 事件驅動
- 高併發處理能力

### 2. 連接池管理

- HTTP 連接池
- Redis 連接池（可選）
- 自動連接重用

### 3. 超時配置

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 10000      # 連接超時
        response-timeout: 30s       # 響應超時
```

## 監控與可觀測性

### 1. Actuator 端點

- `/actuator/health` - 健康檢查
- `/actuator/metrics` - 指標數據
- `/actuator/prometheus` - Prometheus 格式指標
- `/actuator/gateway/routes` - 路由信息

### 2. 監控指標

- HTTP 請求統計
- 路由響應時間
- 熔斷器狀態
- 限流統計

### 3. 日誌記錄系統

#### 3.1 統一請求響應日誌記錄（AOP）

本專案使用統一的請求響應日誌記錄系統，自動記錄所有 Controller 方法的請求和響應：

**日誌輸出範例：**
```
🚀 [abc12345] GET /tymgateway/tymb/people/get-all - Started
📝 [abc12345] Request parameters: []
📋 [abc12345] Request headers: User-Agent: Mozilla/5.0..., Content-Type: application/json
✅ [abc12345] GET /tymgateway/tymb/people/get-all - Completed in 150ms
📤 [abc12345] Response: {"success":true,"message":"People retrieved successfully via gRPC","people":[...],"count":153}
```

**功能特點：**
- **自動化記錄**：無需在每個 Controller 中手動添加日誌程式碼
- **請求追蹤**：每個請求都有唯一 ID，方便問題追蹤
- **效能監控**：自動記錄響應時間，幫助發現效能問題
- **安全性**：自動過濾敏感資訊，避免洩露機密資料
- **可配置**：通過日誌級別控制記錄詳情程度

#### 3.2 現有日誌過濾器

所有請求和響應也會被現有的 `LoggingGlobalFilter` 記錄：
```
2024-10-02 15:51:02 Gateway Request: GET /tymb/weapons from /192.168.1.100
2024-10-02 15:51:02 Gateway Response: GET /tymb/weapons - Status: 200 - Duration: 45ms
```

**兩種日誌記錄的區別：**
- **AOP 日誌**：結構化記錄每個 Controller 方法的詳細請求響應資訊
- **Filter 日誌**：記錄網路層級的請求響應資訊，適用於所有路由

## 安全特性

### 1. CORS 配置

限制允許的來源和方法：
```yaml
allowedOrigins:
  - https://your-frontend-domain.com
allowedMethods:
  - GET
  - POST
  - PUT
  - DELETE
```

### 2. Rate Limiting

防止 DDoS 攻擊和濫用：
- 基於 IP 或用戶的限流
- 動態調整限流策略

### 3. Circuit Breaker

保護後端服務：
- 防止級聯失敗
- 快速失敗機制
- 自動恢復

## 擴展性

### 1. 水平擴展

Gateway 無狀態設計，支援水平擴展：
```yaml
replicas: 2  # K8s 中可輕鬆擴展
```

### 2. 負載均衡

支援多後端實例：
- Round Robin
- Weighted Response Time
- Random

### 3. 動態路由

可在運行時添加或修改路由配置。

## 容錯機制

### 1. 重試策略

對失敗的請求自動重試：
```yaml
filters:
  - name: Retry
    args:
      retries: 3
      statuses: BAD_GATEWAY
```

### 2. 超時處理

設置合理的超時時間防止請求堆積。

### 3. 降級響應

當後端不可用時提供友好的錯誤信息。

## 故障排查指南

### 1. 連接問題

檢查：
- Backend 服務是否運行
- 網絡連接是否正常
- 防火牆規則

### 2. 性能問題

分析：
- 響應時間過長的端點
- CPU 和記憶體使用情況
- 後端服務性能

### 3. 錯誤追蹤

使用：
- 日誌關聯 ID（Trace ID）
- 錯誤堆疊跟蹤
- 監控指標分析

## 路由配置

Gateway 轉發以下端點到 Backend：

| 路徑 | 目標服務 | 說明 |
|------|---------|------|
| `/tymb/people/**` | Backend | 人物管理 API |
| `/tymb/people-images/**` | Backend | 人物圖片 API |
| `/tymb/weapons/**` | Backend | 武器管理 API |
| `/tymb/gallery/**` | Backend | 圖庫管理 API |
| `/tymb/ckeditor/**` | Backend | CKEditor 文件上傳 |
| `/tymb/deckofcards/blackjack/**` | Backend | 21點遊戲 API |
| `/tymb/auth/**` | Backend | 認證 API |
| `/tymb/keycloak/**` | Backend | Keycloak 整合 |
| `/tymb/api/request-status/**` | Backend | 異步請求狀態 |
| `/tymb/api/test/async/**` | Backend | 異步測試 API |
| `/tymb/docs/**` | Backend | JavaDoc 文檔 |
| `/tymb/actuator/**` | Backend | Actuator 監控 |
| `/tymb/swagger-ui/**` | Backend | Swagger UI |

## 項目結構

```
ty-multiverse-gateway/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── tw/com/tymgateway/
│   │   │       ├── TYMGatewayApplication.java     # 主應用類
│   │   │       ├── config/
│   │   │       │   └── GatewayConfig.java         # Gateway 配置
│   │   │       ├── controller/
│   │   │       │   └── FallbackController.java    # 熔斷降級控制器
│   │   │       └── filter/
│   │   │           └── LoggingGlobalFilter.java   # 全局日誌過濾器
│   │   └── resources/
│   │       ├── application.yml                     # 主配置文件
│   │       ├── application-local.yml              # 本地環境配置
│   │       └── env/
│   │           ├── local.properties               # 本地環境變數
│   │           └── platform.properties            # 平台環境變數
│   └── test/                                      # 測試代碼
├── k8s/
│   └── deployment.yaml                            # K8s 部署配置
├── Dockerfile                                      # Docker 鏡像構建
├── Jenkinsfile                                     # CI/CD 流程
├── pom.xml                                         # Maven 配置
└── README.md                                       # 本文檔
```

## 本地開發

### 前置要求

- Java 21
- Maven 3.9+
- Redis (可選，用於限流功能)

### 啟動步驟

1. **啟動 Backend 服務**
   ```bash
   cd ../ty-multiverse-backend
   mvn spring-boot:run
   ```

2. **啟動 Gateway**
   ```bash
   mvn spring-boot:run
   ```

   Gateway 將在 `http://localhost:8081` 啟動

3. **訪問測試**
   ```bash
   # 測試健康檢查
   curl http://localhost:8081/actuator/health
   
   # 測試路由轉發（需要 Backend 運行）
   curl http://localhost:8081/tymb/weapons
   ```

### 配置說明

本地開發時，請複製範例配置文件：

```bash
# 複製範例配置文件
cp src/main/resources/env/local.properties.example src/main/resources/env/local.properties
```

然後在 `src/main/resources/env/local.properties` 中修改配置：

```properties
# Backend 服務地址
BACKEND_SERVICE_URL=http://localhost:8080

# 前端地址（CORS）
PUBLIC_FRONTEND_URL=http://localhost:4321

# Redis 配置（可選）
REDIS_HOST=localhost
REDIS_CUSTOM_PORT=6379
REDIS_PASSWORD=
```

## Docker 部署

### 構建鏡像

```bash
docker build -t ty-multiverse-gateway:latest .
```

### 運行容器

```bash
docker run -p 8081:8081 \
  -e BACKEND_SERVICE_URL=http://backend:8080 \
  -e PUBLIC_FRONTEND_URL=http://your-frontend-url \
  ty-multiverse-gateway:latest
```

## Kubernetes 部署

### 部署到 K8s

```bash
cd k8s
kubectl apply -f deployment.yaml -n ty-multiverse
```

### 查看部署狀態

```bash
kubectl get pods -n ty-multiverse -l app=ty-multiverse-gateway
kubectl logs -f -n ty-multiverse -l app=ty-multiverse-gateway
```

### 檢查服務

```bash
kubectl get svc -n ty-multiverse ty-multiverse-gateway-service
```

## 監控與維護

### 健康檢查

```bash
curl http://localhost:8081/actuator/health
```

### 查看路由信息

```bash
curl http://localhost:8081/actuator/gateway/routes
```

### Prometheus 指標

```bash
curl http://localhost:8081/actuator/prometheus
```

## 性能調優

### JVM 參數配置

在 Dockerfile 中已配置：
- `-Xms256m -Xmx512m`: 堆內存大小
- `-XX:+UseG1GC`: 使用 G1 垃圾收集器
- `-XX:MaxGCPauseMillis=200`: GC 暫停時間目標

### 限流配置

在 `application.yml` 中調整：
```yaml
redis-rate-limiter.replenishRate: 100  # 每秒補充令牌數
redis-rate-limiter.burstCapacity: 200  # 令牌桶容量
```

### 熔斷器配置

在 `application.yml` 中調整：
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 100          # 滑動窗口大小
        failureRateThreshold: 50        # 失敗率閾值
        waitDurationInOpenState: 10s    # 熔斷器打開持續時間
```

## 故障排查

### 常見問題

1. **無法連接到 Backend**
   - 檢查 `BACKEND_SERVICE_URL` 配置
   - 確認 Backend 服務正常運行
   - 查看網絡連接和防火牆設置

2. **CORS 錯誤**
   - 檢查 `PUBLIC_FRONTEND_URL` 配置
   - 確認 CORS 配置正確

3. **限流不生效**
   - 確認 Redis 服務正常運行
   - 檢查 Redis 連接配置


## 授權

Copyright © 2024 TY Team

