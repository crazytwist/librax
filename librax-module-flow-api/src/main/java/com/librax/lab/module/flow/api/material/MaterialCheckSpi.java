package com.librax.lab.module.flow.api.material;

/**
 * 物料前置核验 SPI
 * <p>
 * 由 lab 模块实现，flow 引擎在步骤执行前调用。
 * 与资源调度（ResourcePool）的区别：
 * <ul>
 *   <li>资源不足 → 等待下轮调度重试
 *   <li>物料不足 → 直接 FAIL，需要人工补充
 * </ul>
 */
public interface MaterialCheckSpi {

    /**
     * 检查步骤所需物料是否充足
     *
     * @param request 核验请求
     * @return 核验结果，passed=false 时 failReasons 包含缺失明细
     */
    MaterialCheckResult check(MaterialCheckRequest request);
}
