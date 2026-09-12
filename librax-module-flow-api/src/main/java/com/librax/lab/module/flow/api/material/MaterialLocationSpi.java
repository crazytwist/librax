package com.librax.lab.module.flow.api.material;

/**
 * 物料当前位置维护 SPI。
 *
 * <p>由 lab 模块实现，resource 模块在仓储独立搬运回调中调用，
 * 避免 resource 直接依赖 lab。
 */
public interface MaterialLocationSpi {

    /**
     * 查询物料当前绑定的库位。物料不存在或未绑定时返回 {@code null}。
     */
    String getCurrentSlotId(String instanceId);

    /**
     * 将物料实例的当前位置更新为 {@code slotId}。
     *
     * <p>{@code slotId} 为 {@code null} 时解绑库位（同时清空 zoneCode）。
     * 物料不存在时不抛错，返回 {@code false}。
     *
     * @param instanceId 物料实例ID
     * @param slotId     目标库位ID，null 表示解绑
     * @param zoneCode   目标区域；slotId 为 null 时忽略并清空
     * @return true 表示更新到物料记录；false 表示物料不存在
     */
    boolean updateCurrentSlot(String instanceId, String slotId, String zoneCode);
}
