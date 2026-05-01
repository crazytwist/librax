package com.librax.lab.module.resource.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Redis 资源锁底层封装
 * <p>
 * 统一管理三类 key:
 * <ul>
 *   <li>资源锁:{@code resource:lock:{resourceId}} → Hash(holder, zoneCode, acquiredAt)
 *   <li>持有者反向索引:{@code resource:holder:{holderKey}} → Set of resourceId
 *   <li>区域配额计数:{@code resource:zone:{zone}:borrow:{type}} → Int
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisResourceLock {

    private final StringRedisTemplate redis;

    private static final String LOCK_KEY_PREFIX    = "resource:lock:";
    private static final String HOLDER_KEY_PREFIX  = "resource:holder:";
    private static final String QUOTA_KEY_PREFIX   = "resource:zone:";

    private static final String FIELD_HOLDER      = "holder";
    private static final String FIELD_ZONE_CODE   = "zoneCode";
    private static final String FIELD_ACQUIRED_AT = "acquiredAt";

    /**
     * 尝试加锁
     *
     * @param resourceId 资源 ID
     * @param holderKey  持有者
     * @param zoneCode   申请方的区域,用于共享资源释放时减配额(可 null)
     * @param ttlMs      锁超时时间(ms)
     * @return true=加锁成功,false=已被他人持有
     */
    public boolean tryLock(String resourceId, String holderKey, String zoneCode, long ttlMs) {
        String lockKey = lockKey(resourceId);

        Boolean acquired = redis.opsForHash().putIfAbsent(lockKey, FIELD_HOLDER, holderKey);
        if (Boolean.FALSE.equals(acquired)) {
            return false;
        }

        // 补充附加字段并设置 TTL
        Map<String, String> extra = new HashMap<>();
        extra.put(FIELD_ACQUIRED_AT, String.valueOf(System.currentTimeMillis()));
        if (zoneCode != null && !zoneCode.isEmpty()) {
            extra.put(FIELD_ZONE_CODE, zoneCode);
        }
        redis.opsForHash().putAll(lockKey, extra);
        redis.expire(lockKey, Duration.ofMillis(ttlMs));

        // 反向索引
        redis.opsForSet().add(holderKey(holderKey), resourceId);
        redis.expire(holderKey(holderKey), Duration.ofMillis(ttlMs));

        return true;
    }

    /**
     * 释放锁(带持有者校验)
     *
     * @return true=释放成功,false=持有者不匹配或锁已过期
     */
    public boolean release(String resourceId, String holderKey) {
        String lockKey = lockKey(resourceId);
        Object currentHolder = redis.opsForHash().get(lockKey, FIELD_HOLDER);

        if (currentHolder == null) {
            log.debug("[RedisResourceLock] 锁不存在或已过期 resourceId={}", resourceId);
            return false;
        }

        if (!Objects.equals(currentHolder.toString(), holderKey)) {
            log.warn("[RedisResourceLock] 持有者不匹配,拒绝释放 resourceId={} expected={} actual={}",
                    resourceId, holderKey, currentHolder);
            return false;
        }

        redis.delete(lockKey);
        redis.opsForSet().remove(holderKey(holderKey), resourceId);
        return true;
    }

    /**
     * 查锁里记录的 zoneCode(共享资源释放时用)
     *
     * @return 申请时传入的 zoneCode,没有记录返回 null
     */
    public String getLockZone(String resourceId) {
        Object zone = redis.opsForHash().get(lockKey(resourceId), FIELD_ZONE_CODE);
        return zone == null ? null : zone.toString();
    }

    /** 查持有者占用的所有资源 */
    public Set<String> getResourcesByHolder(String holderKey) {
        return redis.opsForSet().members(holderKey(holderKey));
    }

    /** 清理持有者反向索引(releaseByHolder 完成后调) */
    public void clearHolder(String holderKey) {
        redis.delete(holderKey(holderKey));
    }

    /** 区域配额 +1,返回 +1 后的值 */
    public long incrQuota(String zoneCode, String resourceType) {
        Long v = redis.opsForValue().increment(quotaKey(zoneCode, resourceType));
        return v == null ? 0L : v;
    }

    /** 区域配额 -1,兜底防负数 */
    public void decrQuota(String zoneCode, String resourceType) {
        Long v = redis.opsForValue().decrement(quotaKey(zoneCode, resourceType));
        if (v != null && v < 0) {
            redis.opsForValue().set(quotaKey(zoneCode, resourceType), "0");
        }
    }

    /** 查当前区域借用数 */
    public long getQuota(String zoneCode, String resourceType) {
        String v = redis.opsForValue().get(quotaKey(zoneCode, resourceType));
        return v == null ? 0L : Long.parseLong(v);
    }

    private String lockKey(String resourceId) {
        return LOCK_KEY_PREFIX + resourceId;
    }

    private String holderKey(String holder) {
        return HOLDER_KEY_PREFIX + holder;
    }

    private String quotaKey(String zone, String type) {
        return QUOTA_KEY_PREFIX + zone + ":borrow:" + type;
    }
}