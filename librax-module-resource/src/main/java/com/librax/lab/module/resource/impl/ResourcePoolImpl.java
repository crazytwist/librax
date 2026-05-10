package com.librax.lab.module.resource.impl;

import com.librax.lab.module.flow.api.resource.AcquireFailReasonEnum;
import com.librax.lab.module.flow.api.resource.AcquireRequest;
import com.librax.lab.module.flow.api.resource.AcquireResult;
import com.librax.lab.module.flow.api.resource.ResourcePool;
import com.librax.lab.module.resource.core.RedisResourceLock;
import com.librax.lab.module.resource.core.ResourceSelector;
import com.librax.lab.module.resource.dal.dataobject.resourceconfig.ResourceConfigDO;
import com.librax.lab.module.resource.dal.dataobject.stepresourcehold.StepResourceHoldDO;
import com.librax.lab.module.resource.dal.dataobject.zonequota.ZoneQuotaDO;
import com.librax.lab.module.resource.dal.mysql.resourceconfig.ResourceConfigMapper;
import com.librax.lab.module.resource.dal.mysql.stepresourcehold.StepResourceHoldMapper;
import com.librax.lab.module.resource.dal.mysql.zonequota.ZoneQuotaMapper;
import com.librax.lab.module.resource.enums.OwnershipTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 资源池实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourcePoolImpl implements ResourcePool {

    private final ResourceSelector selector;
    private final RedisResourceLock lock;
    private final ResourceConfigMapper resourceConfigMapper;
    private final ZoneQuotaMapper zoneQuotaMapper;
    private final StepResourceHoldMapper stepResourceHoldMapper;

    private static final long DEFAULT_RETRY_MS = 2_000L;
    private static final long TTL_BUFFER_MS = 30_000L;

    // ================================================================
    // acquire
    // ================================================================

    @Override
    public AcquireResult acquire(AcquireRequest req) {
        validate(req);

        ResourceSelector.Candidates candidates = selector.select(req);

        if (candidates.isEmpty()) {
            AcquireFailReasonEnum reason = hasAnyConfig(req) ? AcquireFailReasonEnum.NO_HEALTHY : AcquireFailReasonEnum.NO_CONFIG;
            log.warn("[ResourcePool] 无候选资源 type={} zone={} reason={}",
                    req.getResourceType(), req.getZoneCode(), reason);
            return AcquireResult.fail(reason, DEFAULT_RETRY_MS);
        }

        long ttlMs = req.getHoldTimeoutMs() + TTL_BUFFER_MS;

        // 1. 优先尝试独占资源(本区)
        for (String rid : candidates.exclusive()) {
            if (lock.tryLock(rid, req.getHolderKey(), req.getZoneCode(), ttlMs)) {
                log.info("[ResourcePool] 抢占独占资源 resourceId={} holder={}", rid, req.getHolderKey());
                return AcquireResult.ok(rid);
            }
        }

        // 2. 独占抢不到,尝试共享池(需先校验区域配额)
        if (!candidates.shared().isEmpty()) {
            AcquireResult sharedResult = trySharedAcquire(req, candidates.shared(), ttlMs);
            if (sharedResult != null) return sharedResult;
        }

        // 3. 都没抢到
        boolean hasExclusiveOnly = !candidates.exclusive().isEmpty()
                && candidates.shared().isEmpty();
        AcquireFailReasonEnum reason = hasExclusiveOnly
                ? AcquireFailReasonEnum.ZONE_EXCLUSIVE_BUSY
                : AcquireFailReasonEnum.NO_AVAILABLE;
        log.info("[ResourcePool] 资源抢占失败 type={} zone={} reason={}",
                req.getResourceType(), req.getZoneCode(), reason);
        return AcquireResult.fail(reason, DEFAULT_RETRY_MS);
    }

    /**
     * 共享池抢锁,失败返回 null 交给主流程继续判定
     */
    private AcquireResult trySharedAcquire(AcquireRequest req, List<String> sharedIds, long ttlMs) {
        String zone = req.getZoneCode();
        String type = req.getResourceType();

        // 有 zone 才做配额检查;无 zone 的场景(比如系统级任务)直接跳过
        if (zone != null && !zone.isEmpty()) {
            ZoneQuotaDO quota = zoneQuotaMapper.selectByZoneAndType(zone, type);
            if (quota != null) {
                long current = lock.getQuota(zone, type);
                if (current >= quota.getMaxBorrow()) {
                    log.info("[ResourcePool] 区域配额已满 zone={} type={} current={}/{}",
                            zone, type, current, quota.getMaxBorrow());
                    return AcquireResult.fail(
                            AcquireFailReasonEnum.QUOTA_EXCEEDED, DEFAULT_RETRY_MS);
                }
            }
        }

        // 抢锁
        for (String rid : sharedIds) {
            if (lock.tryLock(rid, req.getHolderKey(), zone, ttlMs)) {
                if (zone != null && !zone.isEmpty()) {
                    lock.incrQuota(zone, type);
                }
                log.info("[ResourcePool] 抢占共享资源 resourceId={} holder={} zone={}",
                        rid, req.getHolderKey(), zone);
                return AcquireResult.ok(rid);
            }
        }
        return null;
    }

    // ================================================================
    // release
    // ================================================================

    @Override
    public void release(String resourceId, String holderKey) {
        ResourceConfigDO config = resourceConfigMapper.selectByResourceId(resourceId);

        // 共享资源:释放前先读 lock 里记录的 zone,用于减配额
        String lockZone = null;
        boolean isShared = config != null
                && OwnershipTypeEnum.SHARED.name().equals(config.getOwnershipType());
        if (isShared) {
            lockZone = lock.getLockZone(resourceId);
        }

        boolean released = lock.release(resourceId, holderKey);
        if (!released) return;

        if (isShared && lockZone != null && !lockZone.isEmpty()) {
            lock.decrQuota(lockZone, config.getResourceType());
            log.info("[ResourcePool] 释放共享资源 resourceId={} holder={} zone={}",
                    resourceId, holderKey, lockZone);
        } else {
            log.info("[ResourcePool] 释放独占资源 resourceId={} holder={}",
                    resourceId, holderKey);
        }
    }

    @Override
    public int releaseByHolder(String holderKey) {
        Set<String> resourceIds = lock.getResourcesByHolder(holderKey);
        if (resourceIds == null || resourceIds.isEmpty()) return 0;

        int count = 0;
        for (String rid : resourceIds) {
            // 逐个走 release,复用配额回退逻辑
            release(rid, holderKey);
            count++;
        }
        lock.clearHolder(holderKey);
        log.info("[ResourcePool] 批量释放 holder={} count={}", holderKey, count);
        return count;
    }

    @Override
    public void recordHold(String executionId, String nodeId, int attempt, String resourceId, String resourceType) {
        StepResourceHoldDO hold = StepResourceHoldDO.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .attempt(attempt)
                .resourceId(resourceId)
                .resourceType(resourceType)
                .acquiredAt(LocalDateTime.now())
                .build();
        stepResourceHoldMapper.insert(hold);
        log.info("[ResourcePool] 写入 hold 记录 executionId={} nodeId={} attempt={} resourceId={}",
                executionId, nodeId, attempt, resourceId);
    }

    @Override
    public void markHoldReleased(String executionId, String nodeId, int attempt, String reason) {
        int rows = stepResourceHoldMapper.markReleased(
                executionId, nodeId, attempt, reason, LocalDateTime.now());
        if (rows == 0) {
            log.warn("[ResourcePool] hold 记录不存在或已释放 executionId={} nodeId={} attempt={}",
                    executionId, nodeId, attempt);
        }
    }

    @Override
    public String queryHeldResourceId(String executionId, String nodeId, int attempt) {
        StepResourceHoldDO hold = stepResourceHoldMapper
                .selectActiveHold(executionId, nodeId, attempt);
        return hold != null ? hold.getResourceId() : null;
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    private void validate(AcquireRequest req) {
        if (req.getResourceType() == null || req.getResourceType().isEmpty()) {
            throw new IllegalArgumentException("resourceType 不能为空");
        }
        if (req.getHolderKey() == null || req.getHolderKey().isEmpty()) {
            throw new IllegalArgumentException("holderKey 不能为空");
        }
        if (req.getHoldTimeoutMs() <= 0) {
            throw new IllegalArgumentException("holdTimeoutMs 必须大于 0");
        }
    }

    private boolean hasAnyConfig(AcquireRequest req) {
        boolean hasShared = !resourceConfigMapper.selectSharedByType(req.getResourceType()).isEmpty();
        if (hasShared) return true;
        if (req.getZoneCode() == null || req.getZoneCode().isEmpty()) return false;
        return !resourceConfigMapper.selectExclusiveByTypeAndZone(
                req.getResourceType(), req.getZoneCode()).isEmpty();
    }
}