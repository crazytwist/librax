package com.librax.lab.module.device.api;

import java.util.List;

/**
 * 设备查询 SPI
 * <p>
 * device 模块实现,resource 模块通过此接口查设备清单和健康状态。
 * <p>
 * 只暴露"只读查询"方法,不暴露 DeviceGateway 等内部实现。
 * resource 模块拿到设备列表后,自己决定选哪一台做锁。
 *
 * <p>为什么不让 resource 直接查 lab_device_info 表:
 * <ul>
 *   <li>设备健康状态在 Redis 里缓存,直接查表拿不到
 *   <li>device 模块未来会加设备权限、禁用等业务逻辑,集中在此
 *   <li>保持 resource 不依赖 device 的 DAL 层
 * </ul>
 */
public interface DeviceQueryApi {

    /**
     * 按类型 + 区域查可用设备清单
     * <p>
     * "可用"定义:enabled=1 且 online=true 且 healthStatus 不是 FAULT
     * <p>
     * zoneCode 语义:
     * <ul>
     *   <li>非空 → 只返回该区域的设备</li>
     *   <li>null → 返回所有区域的设备(resource 模块会按独占/共享规则进一步过滤)</li>
     * </ul>
     *
     * @param deviceType 设备类型,如 PH_METER / AGV
     * @param zoneCode   期望区域,null 表示不限
     * @return 设备 ID 列表,按"最近最少使用"排序(负载均衡基础),无匹配返回空列表
     */
    List<String> listAvailable(String deviceType, String zoneCode);

    /**
     * 查单个设备的健康状态
     * <p>
     * resource 模块在抢锁前再校验一次健康(防止 listAvailable 返回后设备刚好离线)。
     * 比较轻量,内部从 Redis 读缓存,毫秒级返回。
     *
     * @param deviceId 设备 ID
     * @return 健康快照,设备不存在时返回 OFFLINE 状态(不抛异常)
     */
    DeviceHealthView getHealth(String deviceId);

    /**
     * 查设备所属区域
     * <p>
     * resource 模块校验"独占资源必须本区使用"时用。
     *
     * @param deviceId 设备 ID
     * @return 区域编码,设备不存在返回 null
     */
    String getZoneCode(String deviceId);
}