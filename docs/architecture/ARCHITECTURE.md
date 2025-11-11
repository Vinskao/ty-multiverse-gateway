# TY Multiverse Gateway 架構文檔

## 系統架構圖

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
└──────────────┬──────────────────────────────┘
               │
               │ Load Balanced HTTP
               ▼
┌─────────────────────────────────────────────┐
│      TY Multiverse Backend Service          │
│         (Spring Boot REST API)              │
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

```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 100
      redis-rate-limiter.burstCapacity: 200
```

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

## 配置管理

### 環境配置

Gateway 支援兩種環境配置：

#### Local 環境（開發）
- Backend URL: `http://localhost:8080`
- Redis: 本地 Redis 實例
- 日誌級別: DEBUG

#### Platform 環境（生產）
- Backend URL: K8s 服務名稱
- Redis: 集群 Redis
- 日誌級別: INFO

### 配置注入

使用 Maven filtering 實現配置注入：
```xml
<filters>
  <filter>src/main/resources/env/${env}.properties</filter>
</filters>
```

## 性能特性

### 1. 反應式架構

基於 Spring WebFlux：
- 非阻塞 I/O
- 事件驅動
- 高併發處理能力

### 2. 連接池管理

- HTTP 連接池
- Redis 連接池
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

### 3. 日誌記錄

所有請求和響應都會被記錄：
```
2024-10-02 15:51:02 Gateway Request: GET /tymb/weapons from /192.168.1.100
2024-10-02 15:51:02 Gateway Response: GET /tymb/weapons - Status: 200 - Duration: 45ms
```

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

## 最佳實踐

### 1. 監控告警

設置 Prometheus + Grafana 監控：
- CPU 使用率
- 記憶體使用率
- 請求響應時間
- 錯誤率

### 2. 日誌管理

使用 ELK 或類似工具：
- 集中化日誌收集
- 日誌分析和查詢
- 異常告警

### 3. 配置管理

使用 ConfigMap 和 Secret：
- 環境變數注入
- 敏感信息保護
- 配置版本控制

### 4. 健康檢查

配置 K8s 健康檢查：
- Liveness Probe
- Readiness Probe
- 自動重啟不健康的 Pod

## 版本升級策略

### 1. 滾動更新

K8s 滾動更新配置：
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0
```

### 2. 灰度發布

使用 Service Mesh 或自定義路由實現灰度發布。

### 3. 回滾機制

保留歷史版本，支援快速回滾：
```bash
kubectl rollout undo deployment/ty-multiverse-gateway
```

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

