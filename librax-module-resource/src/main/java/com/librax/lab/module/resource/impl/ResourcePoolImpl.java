package com.librax.lab.module.resource.impl;

import com.librax.lab.module.flow.api.pipeline.PipelineExecutionSpi;
import com.librax.lab.module.flow.api.resource.AcquireFailReasonEnum;
import com.librax.lab.module.flow.api.resource.AcquireRequest;
import com.librax.lab.module.flow.api.resource.AcquireResult;
import com.librax.lab.module.flow.api.resource.ResourcePool;
import com.librax.lab.module.flow.api.resource.ResourceReleasedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 资源池实现
 *
 * 两种资源申请模式（通过 UnitLauncherBean 的 resource_acquire_mode 参数控制）：
 *
 *   LAZY（默认）：
 *     子节点各自申请各自释放，原有行为完全不变。
 *
 *   PRE_ACQUIRE：
 *     父节点（UnitLauncherBean）在启动子流程前预占所有资源，写 ACQUIRED hold 记录。
 *     子节点申请时，tryInheritFromParent 查到父节点的 ACQUIRED 记录，
 *     写 INHERITED hold 记录并返回，不走竞争。
 *     子节点 release 时，检测到自己的 hold 是 INHERITED，跳过释放。
 *     父节点子流程全部完成后，DAG 引擎对父节点执行 releaseByHolder，统一释放预占资源。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourcePoolImpl implements ResourcePool {

    private final ResourceSelector       selector;
    private final RedisResourceLock      lock;
    private final ResourceConfigMapper   resourceConfigMapper;
    private final ZoneQuotaMapper        zoneQuotaMapper;
    private final StepResourceHoldMapper stepResourceHoldMapper;
    private final PipelineExecutionSpi   executionSpi;
    private final ApplicationEventPublisher eventPublisher;

    private static final long DEFAULT_RETRY_MS = 2_000L;
    private static final long TTL_BUFFER_MS    = 30_000L;

    // ================================================================
    // acquire
    // ================================================================

    @Override
    public AcquireResult acquire(AcquireRequest req) {
        validate(req);

        // 优先查父执行链是否已有可继承的资源锁
        // PRE_ACQUIRE 模式下，父节点已预占，子节点直接继承，不走竞争
        AcquireResult inherited = tryInheritFromParent(req);
        if (inherited != null) return inherited;

        // 原有竞争申请逻辑
        ResourceSelector.Candidates candidates = selector.select(req);

        if (candidates.isEmpty()) {
            AcquireFailReasonEnum reason = hasAnyConfig(req)
                    ? AcquireFailReasonEnum.NO_HEALTHY
                    : AcquireFailReasonEnum.NO_CONFIG;
            log.warn("[ResourcePool] 无候选资源 type={} zone={} reason={}",
                    req.getResourceType(), req.getZoneCode(), reason);
            return AcquireResult.fail(reason, DEFAULT_RETRY_MS);
        }

        long ttlMs = req.getHoldTimeoutMs() + TTL_BUFFER_MS;

        // 1. 优先独占资源
        for (String rid : candidates.exclusive()) {
            if (lock.tryLock(rid, req.getHolderKey(), req.getZoneCode(), ttlMs)) {
                log.info("[ResourcePool] 抢占独占资源 resourceId={} holder={}", rid, req.getHolderKey());
                return AcquireResult.ok(rid);
            }
        }

        // 2. 共享池
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

    // ================================================================
    // release
    // ================================================================

    @Override
    public void release(String resourceId, String holderKey) {
        // holderKey 格式：executionId:nodeId:attempt
        // 拆出来查当前节点自己的 hold 记录，判断是否 INHERITED
        // 注意：这里查的是"当前节点自己的 hold"（可能是 ACQUIRED 或 INHERITED）
        // 不是查 resourceId 对应的 hold，是查当前节点写过什么类型的 hold
        if (StringUtils.hasText(holderKey)) {
            String[] parts = holderKey.split(":");
            if (parts.length >= 3) {
                String executionId = parts[0];
                String nodeId      = parts[1];
                int    attempt;
                try {
                    attempt = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    attempt = 1;
                }

                StepResourceHoldDO hold = stepResourceHoldMapper.selectActiveHold(
                        executionId, nodeId, attempt);

                if (hold != null && "INHERITED".equals(hold.getHoldType())) {
                    // 继承来的资源，子节点不能释放，由父节点统一释放
                    log.debug("[ResourcePool] 继承锁，跳过释放 resourceId={} holderKey={}",
                            resourceId, holderKey);
                    return;
                }
            }
        }

        // 原有释放逻辑
        ResourceConfigDO config = resourceConfigMapper.selectByResourceId(resourceId);

        String  lockZone = null;
        boolean isShared = config != null
                && OwnershipTypeEnum.SHARED.name().equals(config.getOwnershipType());
        if (isShared) {
            lockZone = lock.getLockZone(resourceId);
        }

        boolean released = lock.release(resourceId, holderKey);
        if (!released) return;

        String resourceType = config != null ? config.getResourceType() : null;

        if (isShared && lockZone != null && !lockZone.isEmpty()) {
            lock.decrQuota(lockZone, resourceType);
            log.info("[ResourcePool] 释放共享资源 resourceId={} holder={} zone={}",
                    resourceId, holderKey, lockZone);
        } else {
            log.info("[ResourcePool] 释放独占资源 resourceId={} holder={}",
                    resourceId, holderKey);
        }

        // 发布释放事件，唤醒所有等待该资源类型的 PENDING 步骤重新调度
        if (resourceType != null) {
            eventPublisher.publishEvent(
                    new ResourceReleasedEvent(this, resourceId, resourceType, lockZone));
            log.debug("[ResourcePool] 发布 ResourceReleasedEvent type={} zone={}",
                    resourceType, lockZone);
        }
    }

    @Override
    public int releaseByHolder(String holderKey) {
        Set<String> resourceIds = lock.getResourcesByHolder(holderKey);
        if (resourceIds == null || resourceIds.isEmpty()) return 0;

        int count = 0;
        for (String rid : resourceIds) {
            // 父节点统一释放时，hold 类型是 ACQUIRED，可以正常释放
            // 这里直接走 Redis 释放，不过 hold 判断（父节点释放的是自己 ACQUIRED 的锁）
            releaseDirectly(rid, holderKey);
            count++;
        }
        lock.clearHolder(holderKey);
        log.info("[ResourcePool] 批量释放 holder={} count={}", holderKey, count);
        return count;
    }

    @Override
    public void recordHold(String executionId, String nodeId, int attempt,
                           String resourceId, String resourceType) {
        StepResourceHoldDO hold = StepResourceHoldDO.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .attempt(attempt)
                .resourceId(resourceId)
                .resourceType(resourceType)
                .holdType("ACQUIRED")
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
        StepResourceHoldDO hold = stepResourceHoldMapper.selectActiveHold(
                executionId, nodeId, attempt);
        return hold != null ? hold.getResourceId() : null;
    }

    // ================================================================
    // 父子执行链资源继承
    // ================================================================

    /**
     * 尝试从父执行链继承资源锁
     *
     * 执行逻辑：
     *   1. 通过 req.executionId 往上查 parent_execution_id（最多 5 层，防无限递归）
     *   2. 对每层父执行ID，查 pe_step_resource_hold 是否有同类型的 ACQUIRED 活跃记录
     *   3. 找到则：
     *      - 写一条 INHERITED 类型 hold 记录（release 时检测到 INHERITED 直接跳过）
     *      - 返回 AcquireResult.inherited(resourceId)
     *   4. 找不到则返回 null，继续走正常竞争申请
     *
     * 注意：只查 ACQUIRED 记录，不查 INHERITED，防止继承链循环
     */
    private AcquireResult tryInheritFromParent(AcquireRequest req) {
        // executionId 为空（系统级任务）不走继承
        if (!StringUtils.hasText(req.getExecutionId())) return null;

        String checkId  = req.getExecutionId();
        int    maxDepth = 5;

        for (int depth = 0; depth < maxDepth; depth++) {
            String parentId = executionSpi.selectParentExecutionId(checkId);
            if (!StringUtils.hasText(parentId)) break;

            // 查父执行ID下，同类型的 ACQUIRED 活跃 hold
            StepResourceHoldDO parentHold =
                    stepResourceHoldMapper.selectActiveHoldByExecutionAndType(
                            parentId,
                            req.getResourceType());

            if (parentHold != null) {
                String resourceId = parentHold.getResourceId();

                // 写 INHERITED 记录，标记"我用的是父节点的锁，release 时跳过"
                StepResourceHoldDO inheritedHold = StepResourceHoldDO.builder()
                        .executionId(req.getExecutionId())
                        .nodeId(req.getNodeId())
                        .attempt(req.getAttempt())
                        .resourceId(resourceId)
                        .resourceType(req.getResourceType())
                        .holdType("INHERITED")
                        .inheritedFrom(parentId)
                        .acquiredAt(LocalDateTime.now())
                        .build();
                stepResourceHoldMapper.insert(inheritedHold);

                log.info("[ResourcePool] 继承父流程资源锁 resourceId={} type={} " +
                                "parentExecution={} childExecution={}",
                        resourceId, req.getResourceType(),
                        parentId, req.getExecutionId());

                return AcquireResult.inherited(resourceId);
            }

            checkId = parentId;
        }

        return null;
    }

    /**
     * 直接释放（不做 INHERITED 判断）
     * 专供 releaseByHolder 调用，父节点批量释放自己 ACQUIRED 的锁时使用
     */
    private void releaseDirectly(String resourceId, String holderKey) {
        ResourceConfigDO config = resourceConfigMapper.selectByResourceId(resourceId);

        String  lockZone = null;
        boolean isShared = config != null
                && OwnershipTypeEnum.SHARED.name().equals(config.getOwnershipType());
        if (isShared) {
            lockZone = lock.getLockZone(resourceId);
        }

        boolean released = lock.release(resourceId, holderKey);
        if (!released) return;

        if (isShared && lockZone != null && !lockZone.isEmpty()) {
            lock.decrQuota(lockZone, config.getResourceType());
            log.info("[ResourcePool] 批量释放共享资源 resourceId={} holder={} zone={}",
                    resourceId, holderKey, lockZone);
        } else {
            log.info("[ResourcePool] 批量释放独占资源 resourceId={} holder={}",
                    resourceId, holderKey);
        }
    }

    // ================================================================
    // 原有私有方法
    // ================================================================

    private AcquireResult trySharedAcquire(AcquireRequest req, List<String> sharedIds, long ttlMs) {
        String zone = req.getZoneCode();
        String type = req.getResourceType();

        if (zone != null && !zone.isEmpty()) {
            ZoneQuotaDO quota = zoneQuotaMapper.selectByZoneAndType(zone, type);
            if (quota != null) {
                long current = lock.getQuota(zone, type);
                if (current >= quota.getMaxBorrow()) {
                    log.info("[ResourcePool] 区域配额已满 zone={} type={} current={}/{}",
                            zone, type, current, quota.getMaxBorrow());
                    return AcquireResult.fail(AcquireFailReasonEnum.QUOTA_EXCEEDED, DEFAULT_RETRY_MS);
                }
            }
        }

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

    private void validate(AcquireRequest req) {
        if (req.getResourceType() == null || req.getResourceType().isEmpty())
            throw new IllegalArgumentException("resourceType 不能为空");
        if (req.getHolderKey() == null || req.getHolderKey().isEmpty())
            throw new IllegalArgumentException("holderKey 不能为空");
        if (req.getHoldTimeoutMs() <= 0)
            throw new IllegalArgumentException("holdTimeoutMs 必须大于 0");
    }

    private boolean hasAnyConfig(AcquireRequest req) {
        boolean hasShared = !resourceConfigMapper.selectSharedByType(req.getResourceType()).isEmpty();
        if (hasShared) return true;
        if (req.getZoneCode() == null || req.getZoneCode().isEmpty()) return false;
        return !resourceConfigMapper.selectExclusiveByTypeAndZone(
                req.getResourceType(), req.getZoneCode()).isEmpty();
    }
}