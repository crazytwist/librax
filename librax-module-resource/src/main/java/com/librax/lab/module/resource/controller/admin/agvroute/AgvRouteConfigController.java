package com.librax.lab.module.resource.controller.admin.agvroute;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.module.resource.controller.admin.agvroute.vo.AgvRouteConfigRespVO;
import com.librax.lab.module.resource.controller.admin.agvroute.vo.AgvRouteConfigSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.agvroute.AgvRouteConfigDO;
import com.librax.lab.module.resource.service.agvroute.AgvRouteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - AGV 路由配置。
 *
 * <p>用于维护 AGV 搬运路线的三步动作配置（step1 装载 → step2 移动 → step3 卸载）。
 * 补料与下料服务在启动 AGV 流程时通过 {@code routeCode} 动态读取配置，
 * 工作流编码不再硬编码于业务逻辑中。
 *
 * <p><b>接口概览：</b>
 * <pre>
 * POST   /resource/agv-route-config/create    新增路由配置
 * PUT    /resource/agv-route-config/update    修改路由配置
 * DELETE /resource/agv-route-config/delete    删除路由配置（逻辑删除）
 * GET    /resource/agv-route-config/get       按ID查询
 * GET    /resource/agv-route-config/list      查询全部（含已禁用）
 * GET    /resource/agv-route-config/list-enabled  查询已启用
 * </pre>
 *
 * <p><b>典型路由配置示例（来源：AGV工作流词典）：</b>
 * <pre>
 * routeCode=HCL_TO_WAREHOUSE（下料：后处理→仓储）
 *   step1TaskName=HCL_AGV, step2StationName=WAREHOUSE, step3TaskName=AGV_CC
 *
 * routeCode=WAREHOUSE_TO_HCL（补料：仓储→后处理）
 *   step1TaskName=CC_AGV, step2StationName=HCL_STATION, step3TaskName=AGV_HCL
 * </pre>
 */
@Tag(name = "管理后台 - AGV路由配置",
        description = "维护AGV搬运路线的step1/step2/step3工作流编码，补料/下料服务动态读取，无需硬编码")
@RestController
@RequestMapping("/resource/agv-route-config")
@Validated
public class AgvRouteConfigController {

    @Resource
    private AgvRouteConfigService agvRouteConfigService;

    /**
     * 新增 AGV 路由配置。
     *
     * <p>{@code routeCode} 为业务唯一键，同一编码不可重复创建。
     * 配置保存后，补料/下料服务即可通过该 routeCode 读取 step1/step2/step3 工作流编码。
     */
    @PostMapping("/create")
    @Operation(summary = "新增AGV路由配置",
            description = "routeCode唯一不可重复；enabled=false可提前创建但暂不启用。")
    @PreAuthorize("@ss.hasPermission('resource:agv-route-config:create')")
    public CommonResult<Long> createAgvRouteConfig(@Valid @RequestBody AgvRouteConfigSaveReqVO saveReqVO) {
        return success(agvRouteConfigService.createAgvRouteConfig(saveReqVO));
    }

    /**
     * 修改 AGV 路由配置。
     *
     * <p>{@code routeCode} 不可修改（即使传入也会忽略）。
     * 修改 {@code enabled=false} 可临时停用路由而不删除配置。
     */
    @PutMapping("/update")
    @Operation(summary = "修改AGV路由配置",
            description = "id必填；routeCode不可修改，传了也忽略；enabled=false可临时停用。")
    @PreAuthorize("@ss.hasPermission('resource:agv-route-config:update')")
    public CommonResult<Boolean> updateAgvRouteConfig(@Valid @RequestBody AgvRouteConfigSaveReqVO saveReqVO) {
        agvRouteConfigService.updateAgvRouteConfig(saveReqVO);
        return success(true);
    }

    /**
     * 删除 AGV 路由配置（逻辑删除）。
     *
     * <p>删除后补料/下料服务将无法通过该 routeCode 获取配置，
     * 请确认无正在运行的流程再操作。
     *
     * @param id 路由配置主键
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除AGV路由配置（逻辑删除）",
            description = "删除后该routeCode将不可用，请确认无正在运行的AGV流程再操作。")
    @Parameter(name = "id", description = "主键ID", required = true)
    @PreAuthorize("@ss.hasPermission('resource:agv-route-config:delete')")
    public CommonResult<Boolean> deleteAgvRouteConfig(@RequestParam("id") Long id) {
        agvRouteConfigService.deleteAgvRouteConfig(id);
        return success(true);
    }

    /**
     * 按 ID 查询 AGV 路由配置详情。
     *
     * @param id 路由配置主键
     */
    @GetMapping("/get")
    @Operation(summary = "按ID查询AGV路由配置详情")
    @Parameter(name = "id", description = "主键ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('resource:agv-route-config:query')")
    public CommonResult<AgvRouteConfigRespVO> getAgvRouteConfig(@RequestParam("id") Long id) {
        AgvRouteConfigDO config = agvRouteConfigService.getAgvRouteConfig(id);
        return success(BeanUtils.toBean(config, AgvRouteConfigRespVO.class));
    }

    /**
     * 查询全部 AGV 路由配置（含已禁用，不含已删除），用于管理端列表展示。
     */
    @GetMapping("/list")
    @Operation(summary = "查询全部AGV路由配置",
            description = "返回所有未删除的路由配置（含已禁用），按ID升序排列。")
    @PreAuthorize("@ss.hasPermission('resource:agv-route-config:query')")
    public CommonResult<List<AgvRouteConfigRespVO>> getAgvRouteConfigList() {
        List<AgvRouteConfigDO> list = agvRouteConfigService.getAgvRouteConfigList();
        return success(BeanUtils.toBean(list, AgvRouteConfigRespVO.class));
    }

    /**
     * 查询已启用的 AGV 路由配置，等同于补料/下料服务当前可选取的路由范围。
     */
    @GetMapping("/list-enabled")
    @Operation(summary = "查询已启用的AGV路由配置",
            description = "仅返回enabled=true的路由，即当前可被补料/下料服务使用的配置。")
    @PreAuthorize("@ss.hasPermission('resource:agv-route-config:query')")
    public CommonResult<List<AgvRouteConfigRespVO>> getEnabledAgvRouteConfigList() {
        List<AgvRouteConfigDO> list = agvRouteConfigService.getEnabledAgvRouteConfigList();
        return success(BeanUtils.toBean(list, AgvRouteConfigRespVO.class));
    }
}
