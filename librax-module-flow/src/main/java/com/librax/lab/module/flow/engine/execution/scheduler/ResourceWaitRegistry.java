package com.librax.lab.module.flow.engine.execution.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 资源等待注册表
 *
 * <p>步骤申请资源失败时，将 executionId 注册到对应资源类型的等待集合。
 * 资源释放时，监听器通过 {@link #popAll} 取出所有等待者，触发重新调度。
 *
 * <p>Redis Key：{@code flow:resource:waiting:{resourceType}:{zone}}
 * Value：Set&lt;executionId&gt;
 * TTL：15 分钟（资源等待超时默认 5 分钟，15 分钟为安全兜底，防止 key 泄漏）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceWaitRegistry {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX  = "flow:resource:waiting:";
    private static final long   TTL_MINUTES = 15L;

    /**
     * 注册 executionId 正在等待指定资源类型。
     * 幂等：同一 executionId 多次注册，Set 去重，不会产生副作用。
     */
    public void register(String executionId, String resourceType, String zoneCode) {
        String key = buildKey(resourceType, zoneCode);
        redisTemplate.opsForSet().add(key, executionId);
        redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("[ResourceWaitRegistry] 注册等待 executionId={} type={} zone={}",
                executionId, resourceType, zoneCode);
    }

    /**
     * 取出并清空等待该资源类型的所有 executionId。
     *
     * <p>先取成员后删 key，保证调用方拿到完整列表，且不会被下一次释放重复唤醒。
     * 并发安全：最坏情况两次释放都能取到，各自唤醒一遍；{@code doSchedule} 内部幂等，多唤醒无害。
     */
    public Set<String> popAll(String resourceType, String zoneCode) {
        String key = buildKey(resourceType, zoneCode);
        Set<String> members = redisTemplate.opsForSet().members(key);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        redisTemplate.delete(key);
        Set<String> result = new HashSet<>(members);
        log.debug("[ResourceWaitRegistry] popAll type={} zone={} count={}",
                resourceType, zoneCode, result.size());
        return result;
    }

    /**
     * 流程彻底结束（SUCCESS / FAILED / DEAD）时清理残留注册，防止已结束的 executionId 被唤醒。
     *
     * <p>用 SCAN 匹配全部等待 key，逐一删除该 executionId。
     * 正常路径下（正常完成、超时）流程等待集合已被 popAll 清空，此方法为兜底。
     */
    public void unregisterAll(String executionId) {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return;
        for (String key : keys) {
            Long removed = redisTemplate.opsForSet().remove(key, executionId);
            if (removed != null && removed > 0) {
                log.debug("[ResourceWaitRegistry] 清理注册 executionId={} key={}", executionId, key);
            }
        }
    }

    private String buildKey(String resourceType, String zoneCode) {
        String zone = (zoneCode != null && !zoneCode.isEmpty()) ? zoneCode : "global";
        return KEY_PREFIX + resourceType + ":" + zone;
    }
}
