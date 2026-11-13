# 階段 4：Maya 端點分類（2026-11-13 盤點）

來源：`scripts/list_maya_routes.py` 掃 FastAPI 路由表（167 條）＋人工抽查。路徑是 app 內路徑；對外再加 `/maya-sawa` 前綴（`/maya-v2`、`/voyeur`、`/paprika` 也各有同名對外前綴，切換前用 curl 逐一確認實際對外路徑）。
**每次動手前重跑腳本**，Maya 新增端點後這份會過期。掃描只看 `Depends(...)` 與 handler 本體的關鍵字，「未見驗證」不等於確定沒驗證，要讀 handler。

## A. 公開讀取（未登入就要能看，Gateway 放行）

| 類別 | 端點 |
|---|---|
| 市場報價與期貨 | `GET /market/taiex-futures`、`GET /market/mini-tsmc-futures`、`GET /market/watch`、`GET /market/timestamps`、`/timestamps/{day}/manifest`、`/timestamps/{day}/proof` |
| 儀表板 | `GET /market/dashboard`、`/dashboard/brief`、`/dashboard/events`、`/dashboard/fundamentals/{code}`、`/dashboard/history/{code}`（持股結構另有擁有者判斷）、`/dashboard/industries`、`/dashboard/industries/unclassified`、`/dashboard/macro`、`/dashboard/macro/nodes`、`/dashboard/overseas/{code}`、`/dashboard/research`、`/dashboard/relative/{code}`、`/dashboard/tx-settlements`、`GET /market/signal-events` |
| 分析（公開讀，擁有者看更多） | `GET /market/dashboard/analysis/{code}`、`GET /market/dashboard/targets/{code}`、`GET /market/reports`、`GET /market/reports/{report_id}`、`GET /market/notifications` |
| Benchmark | `GET /market/benchmark`、`/benchmark/monitor`、`/benchmark/versions` |
| 報告 | `GET /market/reports/claims`、`/reports/collector`、`/reports/fulltext`、`/reports/claim-proposals/sources`、`/reports/claim-proposals/source/{report_id}` |
| 精讀與音訊 | `GET /market/daily-maya`、`GET /market/audio`、`/market/audio/{product_type}/{code}/{revision}`、`GET /market/podcast.xml` |
| 社群圖卡 | `GET /market/publishing/cards/{kind}/{code}/{revision}.png`、`/publishing/diagrams/{code}/{revision}/{index}.png`、`GET /market/publishing/.well-known/oauth-protected-resource` |
| 訂閱（設計上公開） | `POST /market/subscriptions`、`GET /market/subscriptions/confirm`、`GET｜POST /market/subscriptions/unsubscribe` |
| 部落格與文章 | `GET /blog/articles`、`/blog/articles/{slug}`、`GET /paprika/articles`、`/paprika/articles/{article_id}`、`GET /paprika/up` |
| 其他 | `GET /`、`GET /public-openapi.json`、`GET /market/routine-usage.py`、`GET /proxy/leetcode-stats/{username}`、`GET /people/names`、`POST /people/get-all`（查詢用 POST）、`GET /voyeur/count/`、`GET /voyeur/history/`、`POST /voyeur/increment/`、`GET /maya-v2/available-models/`、`/maya-v2/ai-models/`、`/maya-v2/ai-models/{id}`、`/maya-v2/ai-providers/` |

## B. 需要登入（Gateway 驗 Keycloak JWT；細部角色仍由 Maya 判斷）

Maya 依賴：`require_manage_users`、`require_manage_users_request`（擁有者／管理者）、`require_authenticated`、`require_member_request`、`analysis_editor`、`optional_owner` 以外的 owner 檢查。

- `/blog/admin/**`、`/git-commits/**`、`/research-pipeline/**`、`/runpod/cost`
- `/market/admin/**`、`/market/auth-status`、`/market/automation/check`、`/market/usage`、`/market/ibkr`
- `/market/portfolio`、`/market/portfolio/history`
- `GET｜PUT /market/dashboard/custom`、`/dashboard/candidates`、`/dashboard/search`
- `PUT /market/dashboard/analysis/{code}`、`PUT /dashboard/targets/{code}`、`PUT /dashboard/industries/{group_id}`（`analysis_editor`：登入使用者或 agent key 皆可，見 C）
- `/market/notification-receipts`（GET／POST）
- `POST /market/reports`、`POST /market/reports/web`、`DELETE /market/reports/{id}`、`POST /market/reports/{id}/review`、`GET /market/reports/review-queue`
- `/market/publishing/social/**`
- `GET｜POST /market/publishing/mcp`：**專用 audience 的 token**，Maya middleware 刻意不把它當一般身分，Gateway 不可用一般 JWT 規則驗它，原樣放行給 Maya
- `/paprika/articles` 的所有寫入（POST／PUT／DELETE、batch、sync、vectorize、purge-deleted）

## C. 機器呼叫（agent key／ChatGPT 無 key 投遞；Gateway 放行，不驗 JWT）

雲端排程帶 `X-Market-Agent-Key`；Gateway 只能轉發，不可記錄或改寫這個 header。

- 帶 agent key：`PUT /market/benchmark/{code}`、`/benchmark/{code}/selection`、`/benchmark/monitor/{code}`、`GET /market/daily-maya/proposals`、`POST /market/daily-maya/proposals/{id}/review`、`GET /market/reports/claim-proposals`、`POST /market/reports/claim-proposals/{report_id}`、`POST /market/reports/claim-proposals/{proposal_id}/review`、`GET /market/reports/pending`、`GET /market/reports/search`、`PUT /market/reports/{report_id}/claims`、`POST /market/notify`、`POST /market/routine-usage`、`POST /market/signal-events`、`POST /market/watch`、`POST /market/watch/weekly`、`GET /market/watch/weekly/proposals`、`POST /market/watch/weekly/proposals/{id}/review`、`POST /otel/v1/metrics`（另有 IP allowlist）
- 刻意無 key 的投遞箱（只存成 pending，不改網站）：`POST /market/daily-maya/proposals`（ChatGPT 每日草稿）、`POST /market/watch/weekly/proposals`。Gateway 對這兩條加嚴格限流即可。
- 混用：上面 B 的 `analysis_editor` 端點也接受 agent key，所以 Gateway 對它們**不可強制 JWT**；規則是「有 Bearer 就驗，沒有就交給 Maya 驗 agent key」。

## D. 叢集內專用（Gateway 一律拒絕，回 404）

只靠 `require_internal_secret` 一個共享密鑰保護，但目前從外網打得到。呼叫者全在叢集內（frontend SSR、backend、CronJob），不經 Gateway，所以在 Gateway 擋掉不影響任何功能：

- `/market/internal/**`（activity、ibkr、portfolio、portfolio/history、reader-events、usage）
- `/runpod/internal/**`

**這一項不必等階段 4，風險低、收益高，可以最先做。**

## E. 掃描未見驗證的寫入或付費端點（先逐一讀 handler 再分類）

這些在 Depends 與 handler 本體都沒找到驗證關鍵字。有的可能是刻意公開，有的可能是漏洞，**未確認前不要宣稱已被保護，也不要直接封掉**（前端或其他服務可能在用）：

| 端點 | 疑慮 | 已知呼叫者 |
|---|---|---|
| `POST /maya-v2/ask-with-model/`、`POST /qa/query` | 每次呼叫花 OpenAI 費用 | 前端問答（`utils/api.ts`） |
| `POST /maya-v2/add-model/` | 寫入模型設定 | 未知 |
| `/maya-v2/conversations/**`（含 DELETE、send_message） | 讀寫他人對話 | 未知 |
| `GET｜DELETE /qa/chat-history/{user_id}`、`/qa/chat-stats/{user_id}`、`/qa/chat-users`、`/maya-v2/qa/chat-history/{session_id}`、`/maya-sawa/qa/chat-history/{session_tail}` | 列出或刪除他人聊天紀錄 | 前端（`qa/chat-history`） |
| `POST /qa/sync-articles`、`/qa/sync-from-api`、`/qa/sync-people-weapons`、`/qa/convert-to-vector`、`/qa/stop-sync`、`GET /qa/stats` | 觸發同步與向量化（耗 CPU 與 embedding 費用）；只有 `enforce_qa_rate_limit` | 可能是 CronJob |
| `POST /qa/search-people` | 查詢 | 未知 |
| `POST /videos/merge-videos`、`GET /videos/download/{job_id}/{ext}` | 最大 1GB 上傳、吃 CPU | 前端 Live 影片 |
| `POST /voyeur/push/` | 寫入 | 前端 `auth.ts` |
| `GET /maya-v2/task-status/{task_id}` | 讀任務結果 | 前端 |
| `GET /qa/sync-config` | 讀設定 | 未知 |

處理方式：每一條決定歸到 A（公開）、B（登入）、C（機器）或 D（內部），結論寫回這份檔案，並在 Maya 補上對應的驗證與測試（`tdd-workflow`）。付費與刪除類優先。

## Maya 既有防護（Gateway 不要重做或打架）

`maya_sawa/core/security.py` 的 `SecurityMiddleware` 已經做了：

- body 大小上限：一般 2MB，上傳路徑另設上限
- 有 Bearer 就驗 Keycloak token，失敗回 401
- Redis 限流：每分鐘、每路徑；未登入每個 IP 60 次、登入每位使用者 180 次
- IP allowlist：`/git-commits/ingest`、`/otel/v1/metrics`

Gateway 的限流定位成**粗粒度的總量保護**（例如每個 IP 每分鐘 300 次），細部限流留在 Maya，避免兩層規則互相打架、難以排查。

## ⚠ 來源 IP 依賴 trusted proxy 網段（階段 4 前必修）

Maya 只在直接連線端屬於 `SECURITY_TRUSTED_PROXY_CIDRS`（目前 `127.0.0.1/32,10.244.1.0/24`）時才採用 `X-Forwarded-For`。

- 2026-11-13 Gateway pod IP 是 10.244.1.164，**剛好**落在網段內，所以現在正常。
- Gateway 若被排到另一台 node（例如 10.244.0.x），Maya 會把所有外部流量都當成 Gateway 一個 IP：
  - 60 次／分鐘的公開限流變成全站共用，到處回 429；
  - IP allowlist 判斷失準。
- 修法（擇一，要經使用者同意）：
  - trusted proxy 改成整個 pod 網段 `10.244.0.0/16`。代價是叢集內任何 pod 都能偽造 XFF，但 Maya 已無外部入口，可接受。
  - 或固定 Gateway 的排程位置。這比較脆弱，不建議。
- 驗證：`kubectl logs deploy/maya-sawa | grep client_ip=` 要看到真實外部 IP，而不是 10.244.x.x。

## 階段 4 執行順序（每步獨立部署、可回退）

1. **D 先做**：Gateway 對 `/maya-sawa/market/internal/**`、`/maya-sawa/runpod/internal/**` 回 404，並寫測試。
2. **修 trusted proxy 網段**，見上一節。
3. **處理 E 表**：逐條確認後分類；在 Maya 補驗證與測試。
4. **Gateway 驗證規則**，判斷順序是 D 拒絕 → A 放行 → C 放行 → B 有 Bearer 就驗、沒有就交給 Maya → 其餘預設拒絕。用白名單，不用黑名單。
   - 外部帶進來的 `X-User`、`X-Roles` 一律刪掉，只有 Gateway 驗證後才自己加。
   - Maya 先**同時**接受 header 與自行驗 Bearer，觀察一週沒有差異再考慮拿掉自驗。
5. **Gateway 粗粒度限流**：Redis `RequestRateLimiter`，`application.yml` 已有被註解的 Redis 設定。
6. 每一步都要過一遍驗證：
   - 未登入：A 全部 200，B 全部 401，D 全部 404；
   - agent key：C 正常（看下一次排程的執行結果）；
   - 擁有者登入：B 正常。
