package com.librax.lab.module.resource.util;

import lombok.extern.slf4j.Slf4j;

import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 按递增间隔重试。
 *
 * <p>第 n 次失败后的等待为 {@code initialIntervalMs * multiplier^(n-1)}。
 * 例如 initialIntervalMs=2000、multiplier=3：第一次失败后等 2s，第二次失败后等 6s。
 *
 * <pre>
 * Result result = RetryUtils.execute(
 *         "开门",
 *         () -> deviceCommandSpi.send(...),
 *         this::accepted,
 *         3,      // 最大尝试次数，含首次
 *         2000L,  // 第一次重试前等待
 *         3);     // 之后每次间隔乘 3
 * </pre>
 */
@Slf4j
public final class RetryUtils {

    private RetryUtils() {
    }

    /**
     * 执行动作，未成功则按递增间隔重试，直到成功或达到最大尝试次数。
     *
     * @param scene             日志中的场景名，便于对照业务
     * @param action            每次尝试执行的动作
     * @param success           结果是否成功；为 true 时立即返回
     * @param maxAttempts       最大尝试次数（含首次），必须 &gt;= 1
     * @param initialIntervalMs 第一次失败后的等待毫秒数，必须 &gt;= 0
     * @param multiplier        后续间隔的倍数，必须 &gt;= 1；大于 1 时间隔递增
     * @return 首次成功的结果；耗尽次数或等待被中断时返回最后一次结果
     */
    public static <T> T execute(String scene,
                                Supplier<T> action,
                                Predicate<T> success,
                                int maxAttempts,
                                long initialIntervalMs,
                                int multiplier) {
        return execute(scene, action, success, maxAttempts, initialIntervalMs, multiplier, RetryUtils::sleep);
    }

    static <T> T execute(String scene,
                         Supplier<T> action,
                         Predicate<T> success,
                         int maxAttempts,
                         long initialIntervalMs,
                         int multiplier,
                         LongConsumer pause) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts 必须 >= 1，实际: " + maxAttempts);
        }
        if (initialIntervalMs < 0) {
            throw new IllegalArgumentException("initialIntervalMs 不能为负，实际: " + initialIntervalMs);
        }
        if (multiplier < 1) {
            throw new IllegalArgumentException("multiplier 必须 >= 1，实际: " + multiplier);
        }

        T result = null;
        long intervalMs = initialIntervalMs;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            result = action.get();
            if (success.test(result)) {
                return result;
            }
            if (attempt >= maxAttempts) {
                log.warn("[RetryUtils] {} 第 {}/{} 次仍未成功，停止重试", scene, attempt, maxAttempts);
                break;
            }
            log.warn("[RetryUtils] {} 第 {}/{} 次未成功，{}ms 后重试", scene, attempt, maxAttempts, intervalMs);
            pause.accept(intervalMs);
            if (Thread.currentThread().isInterrupted()) {
                log.warn("[RetryUtils] {} 重试等待被中断，停止后续尝试", scene);
                break;
            }
            intervalMs = nextInterval(intervalMs, multiplier);
        }
        return result;
    }

    private static long nextInterval(long intervalMs, int multiplier) {
        if (intervalMs == 0 || multiplier == 1) {
            return intervalMs;
        }
        if (intervalMs > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return intervalMs * multiplier;
    }

    private static void sleep(long intervalMs) {
        if (intervalMs <= 0) {
            return;
        }
        try {
            Thread.sleep(intervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[RetryUtils] 重试等待被中断");
        }
    }
}
