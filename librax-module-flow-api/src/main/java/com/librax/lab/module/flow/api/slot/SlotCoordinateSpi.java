package com.librax.lab.module.flow.api.slot;

/**
 * 库位坐标解析 SPI
 * <p>
 * resource 模块实现，AGV 执行器通过此接口获取源/目标库位的世界坐标，
 * 用于构建 AGV 导航指令。
 * <p>
 * 典型调用场景：
 * <pre>
 *   1. AGV 搬运步骤需要从 slotA 取物到 slotB
 *   2. 执行器调 resolve("slotA") 和 resolve("slotB") 拿坐标
 *   3. 组装 AGV 移动指令：{sourceCoord, targetCoord, localIndex}
 * </pre>
 */
public interface SlotCoordinateSpi {

    /**
     * 根据 slotId 解析库位坐标
     *
     * @param slotId 库位唯一编码
     * @return 库位坐标信息，不存在时返回 null
     */
    SlotCoordinate resolve(String slotId);
}
