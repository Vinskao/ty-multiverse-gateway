package tw.com.tymgateway.service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tw.com.tymgateway.dto.AsyncResultMessage;

/**
 * Gateway 端異步結果註冊器
 *
 * <p>負責管理 requestId 與等待結果的 HTTP 請求之間的關係。</p>
 */
@Service
public class AsyncResultRegistry {

    private static final Logger logger = LoggerFactory.getLogger(AsyncResultRegistry.class);

    private final Map<String, CompletableFuture<AsyncResultMessage>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, AsyncResultMessage> completedResults = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> cleanupTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupScheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "async-result-cleanup");
            thread.setDaemon(true);
            return thread;
        });

    public Mono<AsyncResultMessage> awaitResult(String requestId, Duration timeout) {
        Objects.requireNonNull(requestId, "requestId must not be null");

        AsyncResultMessage completed = completedResults.remove(requestId);
        if (completed != null) {
            cancelCleanupTask(requestId);
            return Mono.just(completed);
        }

        CompletableFuture<AsyncResultMessage> future = new CompletableFuture<>();
        CompletableFuture<AsyncResultMessage> existing = pendingRequests.putIfAbsent(requestId, future);
        CompletableFuture<AsyncResultMessage> targetFuture = existing != null ? existing : future;

        return Mono.fromFuture(targetFuture)
            .timeout(timeout)
            .doFinally(signalType -> pendingRequests.remove(requestId));
    }

    public void complete(AsyncResultMessage resultMessage) {
        if (resultMessage == null || resultMessage.getRequestId() == null) {
            logger.warn("忽略沒有 requestId 的異步結果: {}", resultMessage);
            return;
        }

        String requestId = resultMessage.getRequestId();
        CompletableFuture<AsyncResultMessage> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(resultMessage);
            return;
        }

        completedResults.put(requestId, resultMessage);
        ScheduledFuture<?> cleanupTask = cleanupScheduler.schedule(() -> {
            completedResults.remove(requestId);
            cleanupTasks.remove(requestId);
            logger.warn("異步結果在等待期間未被取得，已自動清理: requestId={}", requestId);
        }, 60, TimeUnit.SECONDS);

        ScheduledFuture<?> previousTask = cleanupTasks.put(requestId, cleanupTask);
        if (previousTask != null) {
            previousTask.cancel(false);
        }
    }

    private void cancelCleanupTask(String requestId) {
        ScheduledFuture<?> task = cleanupTasks.remove(requestId);
        if (task != null) {
            task.cancel(false);
        }
    }
}

