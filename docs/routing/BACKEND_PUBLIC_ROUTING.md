# Backend 公開路由

```mermaid
flowchart LR
  Client[公開網域用戶端] --> Ingress[NGINX Ingress]
  Ingress -->|/tymb 與 TCX 專用路徑| Gateway[Spring Cloud Gateway]
  Gateway -->|保留 /tymb 前綴| Backend[Backend ClusterIP]
```

## 開發方法

1. 先用 `mvn -P platform -Dtest=BackendRoutingTest test` 重現路由缺失與跨來源請求失敗。測試涵蓋 backend 健康檢查、401、預檢及大於 1 MiB 的資料完整性。以 `mvn -P platform test` 驗證既有路由。
2. Backend 繼續驗證權杖並處理 CORS。Gateway 保留原始請求，包括 OPTIONS 預檢。NGINX 傳遞來源資訊，Gateway 附加代理節點。Backend 的現有業務授權與限流沒有使用來源 IP。
3. 專用 Ingress 的 Exact 路徑開放 30 MiB 並停用 request buffering。Gateway 的 RequestSize 檢查 Content-Length，NGINX 對包含 chunked 在內的公開上傳強制上限。Backend 保留串流上限。其餘路徑沿用 ingress-nginx 的預設 1 MiB。
4. 先部署 Gateway 並從叢集內驗證其 200、401 和預檢。從兩份來源 YAML 各抽出一組 Prefix／Exact Ingress，先做 server dry-run，再依新網域、舊網域逐一套用並驗收。
5. 每個網域切換前保留原 Prefix Ingress。回復 Gateway 可用版本時，可用 `kubectl rollout undo deployment/ty-multiverse-gateway --to-revision=<已驗證版本>`。若只有 TCX 專用規則異常，可先用 `kubectl delete ingress <原名稱>-tcx` 回復 1 MiB 上限，Prefix 仍經 Gateway。若需撤下整個入口，先從管理端保留對應規則，再移除該網域的 Prefix／Exact Ingress。
6. 使用既有的 `X-Health-Token` 驗證無效 TCX 回 400，測試內容不得包含真實活動。憑證由既有 Kubernetes secret 注入 backend。從外部確認 200／401／413，並查核沒有 Ingress 指向 backend Service。

## 捷徑端點

```mermaid
flowchart LR
  Request["workout-tcx / daily-lines"] --> Guard["方法、X-Health-Token"]
  Guard -->|"POST 與有效權杖"| Quota["各端點每分鐘 12 次"]
  Quota --> Backend["Backend 再驗證並處理"]
  Guard -->|"POST 的 CORS 預檢"| Backend
```

Gateway 從既有 Kubernetes Secret `health-data-credentials` 讀取 `HEALTH_INGEST_TOKEN`。未設定時，只有這兩條寫入端點回 503，其他路徑照原有設定運作。JWT、資料驗證與寫入仍由 backend 負責。
每條捷徑路由的配額從第一筆有效請求起算，每個 Gateway replica 獨立計數，超額回 429 及 `Retry-After`。正式部署目前為單 replica。無效權杖與預檢不占額度，且限流不依賴代理 IP。
`ShortcutIngestGatewayFilterFactoryTest` 驗證 405、401、429、時間窗重置與預檢，`BackendRoutingTest` 驗證兩條路由套用過濾器且 TCX 保持串流。
