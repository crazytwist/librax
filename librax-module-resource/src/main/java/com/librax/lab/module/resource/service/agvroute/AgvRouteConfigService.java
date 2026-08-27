package com.librax.lab.module.resource.service.agvroute;

import com.librax.lab.module.resource.controller.admin.agvroute.vo.AgvRouteConfigSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.agvroute.AgvRouteConfigDO;

import jakarta.validation.Valid;
import java.util.List;

/**
 * AGV 路由配置 Service 接口。
 *
 * <p>提供两类能力：
 * <ol>
 *   <li><b>管理端 CRUD</b>：供后台页面维护路由配置记录。</li>
 *   <li><b>业务查询</b>：{@link #getRequiredByRouteCode} 供补料/下料服务在启动
 *       AGV 流程前获取路由配置，保证 step1/step2/step3 工作流编码均来自数据库，
 *       不再硬编码于业务代码中。</li>
 * </ol>
 */
public interface AgvRouteConfigService {

    // ── 管理端 CRUD ──────────────────────────────────────────────────────────

    /**
     * 新增 AGV 路由配置。
     *
     * @param saveReqVO 新增信息（{@code id} 不传）
     * @return 新记录主键 ID
     * @throws IllegalArgumentException 若 {@code routeCode} 已存在
     */
    Long createAgvRouteConfig(@Valid AgvRouteConfigSaveReqVO saveReqVO);

    /**
     * 修改 AGV 路由配置。
     * <p>{@code routeCode} 不可修改，其余字段均可更新。
     *
     * @param saveReqVO 修改信息（{@code id} 必填）
     * @throws IllegalArgumentException 若 {@code id} 对应记录不存在
     */
    void updateAgvRouteConfig(@Valid AgvRouteConfigSaveReqVO saveReqVO);

    /**
     * 删除 AGV 路由配置（逻辑删除）。
     *
     * @param id 主键
     * @throws IllegalArgumentException 若记录不存在
     */
    void deleteAgvRouteConfig(Long id);

    /**
     * 按主键查询路由配置。
     *
     * @param id 主键
     * @return 路由配置 DO，不存在则返回 {@code null}
     */
    AgvRouteConfigDO getAgvRouteConfig(Long id);

    /**
     * 查询全部路由配置（含已禁用，不含已删除），用于管理端列表。
     *
     * @return 所有未删除的路由，按 ID 升序
     */
    List<AgvRouteConfigDO> getAgvRouteConfigList();

    /**
     * 查询已启用的路由配置列表。
     *
     * @return 已启用路由，按 ID 升序
     */
    List<AgvRouteConfigDO> getEnabledAgvRouteConfigList();

    // ── 业务查询（供补料/下料服务调用）──────────────────────────────────────

    /**
     * 按路由编码获取路由配置，找不到或已禁用时直接抛出异常。
     *
     * <p>这是供 ReplenishmentService / ReturnService 在启动 AGV 流程前调用的核心入口。
     * 调用方无需处理 {@code null} 的情况，配置缺失时会快速失败，避免流程以空编码启动。
     *
     * <p>示例：
     * <pre>
     *   AgvRouteConfigDO route = agvRouteConfigService.getRequiredByRouteCode("HCL_TO_WAREHOUSE");
     *   String step1TaskName = route.getStep1TaskName(); // "HCL_AGV"
     *   String step2Station  = route.getStep2StationName(); // "WAREHOUSE"
     *   String step3TaskName = route.getStep3TaskName(); // "AGV_CC"
     * </pre>
     *
     * @param routeCode 路由编码，如 {@code HCL_TO_WAREHOUSE}
     * @return 路由配置 DO
     * @throws IllegalStateException 若路由不存在或已禁用
     */
    AgvRouteConfigDO getRequiredByRouteCode(String routeCode);
}
