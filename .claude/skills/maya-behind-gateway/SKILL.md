---
name: maya-behind-gateway
description: 讓 Maya Sawa 不對外暴露、只經 ty-multiverse-gateway（Spring Cloud Gateway）進來：新增或修改 Gateway 轉發 Maya 的路由、把 Ingress 從 maya-sawa 切到 gateway、驗證或限流收進 Gateway。排查「改走 Gateway 後 Maya 401／413／504」「上傳大檔失敗」「排程 agent key 被擋」「CORS header 重複」也用。
---

# Maya 只經 Gateway 對外

完整計畫與階段：`ty-multiverse-gateway/docs/routing/MAYA_BEHIND_GATEWAY_PLAN.md`。動手前先讀，並確認目前在哪個階段。

## 不變的原則

1. **對外網址不變。** `/maya-sawa/`、`/maya-v2/`、`/voyeur/`、`/paprika/` 原樣轉發，不加 `/tymg`、不 RewritePath。前端、雲端排程 prompt（`maya-sawa/routines/*.md`）、ChatGPT 任務、Apps Script 都寫死這些網址。
2. **叢集內呼叫不經 Gateway。** frontend SSR 用 `MAYA_SAWA_INTERNAL_URL=http://maya-sawa/maya-sawa`，maya CronJob、backend activity log 也直連 Service。
3. **先加路由、再切 Ingress、最後才搬驗證。** 順序反了會出現：路由還沒有就斷線，或 Maya 信任 header 卻仍可被直連偽造。
4. **舊網域不可回 301。** peoplesystem.tatdvsonorth.com 仍被 Apps Script、ChatGPT connector 使用。

## 必踩的坑（已查證）

| 症狀 | 原因 | 處理 |
|---|---|---|
| Maya 所有路徑 401 | `SecurityConfig` 結尾 `anyExchange().authenticated()` | 四個前綴在它之前 `permitAll()` |
| 帶 agent key／Maya token 被擋 | `CustomBearerTokenResolver` 解析失敗即拒絕 | Maya 路徑不解析 Bearer，或解析失敗不拒絕；寫測試 |
| 長請求 504 | Gateway httpclient 預設 response-timeout 太短 | Maya 路由 1800s（對齊 nginx proxy-read-timeout） |
| 上傳 413 或記憶體爆 | 前層 nginx body 限制，或有 filter 讀整個 body | nginx 保留 `proxy-body-size: 1024m`、`proxy-request-buffering: off`；Gateway filter 一律不讀 body |
| CORS header 出現兩次 | Gateway `CorsWebFilter` + Maya 自己的 CORS | Maya 路由排除 Gateway CORS，或用 `DedupeResponseHeader` 並測 |
| Maya 看到的 IP 都是 Gateway | 未帶 `X-Forwarded-For` | 確認 Gateway 轉發 XFF、Maya 取第一個 |
| Ingress apply 被拒 | admission webhook 不允許兩個 Ingress 同 host+path | 先改原 Ingress 的 backend，或先刪再建，見計畫階段 2 |
| 寫了 NetworkPolicy 卻沒效果 | 叢集 CNI 是 flannel，不執行 NetworkPolicy | 不要宣稱已鎖住；見計畫階段 3 |

## 檔案位置

- 路由：`ty-multiverse-gateway/src/main/resources/application.yml`（`spring.cloud.gateway.routes`）
- 權限：`ty-multiverse-gateway/src/main/java/tw/com/tymgateway/config/SecurityConfig.java`
- CORS：`.../config/GatewayConfig.java`
- Gateway k8s：`ty-multiverse-gateway/k8s/deployment.yaml`（Service `ty-multiverse-gateway-service:8082`）
- Maya Ingress（舊網域）：`maya-sawa/k8s/deployment.yaml` 的 `maya-sawa-ingress`
- 新網域 Ingress：`peoplesystem-terraform-oke/tymultiverse-domain/mirror-ingress.yaml`（`tymcc-maya-sawa`、`tymcc-portfolio-api-protected`）

## 驗證

- 單元／整合測試（遵守 `tdd-workflow`）：轉發、permitAll、Bearer 不被擋、串流不緩衝、逾時。
- 叢集內：`ssh oke-node "kubectl exec deploy/ty-multiverse-frontend -- wget -qO- http://ty-multiverse-gateway-service:8082/maya-sawa/<公開GET>"`。
- 外網：計畫階段 2 的驗證清單。不要為了測試觸發真的社群發文或付費 API。
- 資源：`ssh oke-node "kubectl top pod"`，Gateway 接近 500m CPU／512Mi 才調整。

## 安全

- 不讀、不印 agent key、Keycloak secret 等任何密鑰；密鑰只走 Jenkins credential → K8s secret。
- 推送：gateway 與 maya-sawa 依各自 repo 的分支慣例；maya-sawa main 即 prod。
- 切 Ingress（階段 2）是對外行為改變，執行前跟使用者確認。
