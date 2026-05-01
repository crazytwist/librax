package com.librax.lab.module.resource.core;

import com.librax.lab.module.device.api.DeviceQueryApi;
import com.librax.lab.module.flow.api.resource.AcquireRequest;
import com.librax.lab.module.resource.dal.dataobject.resourceconfig.ResourceConfigDO;
import com.librax.lab.module.resource.dal.mysql.resourceconfig.ResourceConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 候选资源筛选器
 * <p>
 * 根据 AcquireRequest 和资源配置表,选出"理论上可用"的资源ID列表。
 * 不做锁抢占,不做配额判断,那些是 ResourcePoolImpl 的事。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceSelector {

    private final ResourceConfigMapper resourceConfigMapper;
    private final DeviceQueryApi deviceQueryApi;

    /**
     * 返回两个列表:独占候选、共享候选(已按健康过滤)
     */
    public Candidates select(AcquireRequest req) {
        String type = req.getResourceType();
        String zone = req.getZoneCode();

        List<String> exclusive = Collections.emptyList();
        List<String> shared = Collections.emptyList();

        // 1. 独占候选(需要 zoneCode)
        if (zone != null && !zone.isEmpty()) {
            List<ResourceConfigDO> ex = resourceConfigMapper
                    .selectExclusiveByTypeAndZone(type, zone);
            exclusive = filterHealthy(toIds(ex));
        }

        // 2. 共享候选(允许降级时才查)
        if (req.isAllowSharedFallback()) {
            List<ResourceConfigDO> sh = resourceConfigMapper.selectSharedByType(type);
            shared = filterHealthy(toIds(sh));
        }

        log.debug("[ResourceSelector] type={} zone={} exclusive={} shared={}",
                type, zone, exclusive, shared);

        return new Candidates(exclusive, shared);
    }

    private List<String> toIds(List<ResourceConfigDO> list) {
        List<String> ids = new ArrayList<>(list.size());
        for (ResourceConfigDO d : list) ids.add(d.getResourceId());
        return ids;
    }

    /** 调 DeviceQueryApi 过滤掉不健康的设备 */
    private List<String> filterHealthy(List<String> resourceIds) {
        if (resourceIds.isEmpty()) return resourceIds;
        List<String> ok = new ArrayList<>(resourceIds.size());
        for (String id : resourceIds) {
            if (deviceQueryApi.getHealth(id).isUsable()) {
                ok.add(id);
            } else {
                log.debug("[ResourceSelector] 过滤不健康设备 resourceId={}", id);
            }
        }
        return ok;
    }

    /** 候选结果 */
    public record Candidates(List<String> exclusive, List<String> shared) {
        public boolean isEmpty() {
            return exclusive.isEmpty() && shared.isEmpty();
        }
    }
}