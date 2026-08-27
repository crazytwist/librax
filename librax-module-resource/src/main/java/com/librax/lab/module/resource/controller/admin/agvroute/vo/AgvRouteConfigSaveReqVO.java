package com.librax.lab.module.resource.controller.admin.agvroute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AGV 路由配置 - 新增/修改请求 VO。
 *
 * <p><b>新增时：</b>{@code id} 不传；{@code routeCode}、{@code routeName}、
 * {@code sourceStationCode}、{@code destStationCode}、{@code step1TaskName}、
 * {@code step2TaskName}、{@code step2StationName}、{@code step3TaskName} 为必填。
 *
 * <p><b>修改时：</b>{@code id} 必填，{@code routeCode} 不可修改（传了也忽略）。
 *
 * <p><b>taskName 取值参考（AGV 工作流词典）：</b>
 * <pre>
 *   step1：HCL_AGV / STX_AGV / FX_AGV / CC_AGV
 *   step2：AgvMove（固定值）
 *   step3：AGV_HCL / AGV_STX / AGV_FX / AGV_CC
 * </pre>
 */
@Data
@Schema(description = "AGV路由配置 - 新增/修改请求")
public class AgvRouteConfigSaveReqVO {

    /** 主键 ID，修改时必填，新增时不传。 */
    @Schema(description = "主键ID，修改时必填", example = "1")
    private Long id;

    /**
     * 路由唯一编码，新增时必填，修改时忽略（不支持变更）。
     * <p>建议格式：{起始站}_TO_{目标站}，如 {@code HCL_TO_WAREHOUSE}。
     */
    @Schema(description = "路由唯一编码，新增必填，修改时忽略（不可变更）",
            example = "HCL_TO_WAREHOUSE")
    @NotBlank(message = "路由编码不能为空")
    private String routeCode;

    /** 路由名称，可读描述，如"后处理工站 → 仓储"。 */
    @Schema(description = "路由名称，可读描述",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "后处理工站 → 仓储（下料）")
    @NotBlank(message = "路由名称不能为空")
    private String routeName;

    /**
     * 起始站点编码，对应 AGV 地图区域标识。
     * <p>示例：{@code HCL}、{@code WAREHOUSE}、{@code STX}、{@code FX}。
     */
    @Schema(description = "起始站点编码（对应AGV地图区域标识）",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "HCL")
    @NotBlank(message = "起始站点编码不能为空")
    private String sourceStationCode;

    /**
     * 目标站点编码，对应 AGV 地图区域标识。
     * <p>示例：{@code WAREHOUSE}、{@code HCL}、{@code STX}、{@code FX}。
     */
    @Schema(description = "目标站点编码（对应AGV地图区域标识）",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "WAREHOUSE")
    @NotBlank(message = "目标站点编码不能为空")
    private String destStationCode;

    /**
     * AGV 任务类型，透传给 AGV 设备，由厂商定义，通常为 {@code "1"}。
     */
    @Schema(description = "AGV任务类型（透传AGV设备，厂商定义，通常为1）",
            example = "1")
    private String taskType;

    /**
     * AGV 托板类型，透传给 AGV 设备，由厂商定义，通常为 {@code "1"}。
     */
    @Schema(description = "AGV托板类型（透传AGV设备，厂商定义，通常为1）",
            example = "1")
    private String plateType;

    /**
     * step1 工作流编码：物料装载到 AGV 的动作编码。
     * <p>取值参考 AGV 工作流词典：
     * <ul>
     *   <li>{@code HCL_AGV} - 后处理工站下料到AGV（孔位 1~16）</li>
     *   <li>{@code STX_AGV} - 手套箱工站下料到AGV（孔位 1~8）</li>
     *   <li>{@code FX_AGV}  - 分析工站下料到AGV（孔位 1~3）</li>
     *   <li>{@code CC_AGV}  - 仓储工站下料到AGV（孔位 1~2）</li>
     * </ul>
     */
    @Schema(description = "step1工作流编码：物料装载到AGV（参考AGV工作流词典）",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "HCL_AGV")
    @NotBlank(message = "step1工作流编码不能为空")
    private String step1TaskName;

    /**
     * step2 工作流编码：AGV 整体移动的动作，通常固定为 {@code AgvMove}。
     */
    @Schema(description = "step2工作流编码：AGV整体移动，通常固定为AgvMove",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "AgvMove")
    @NotBlank(message = "step2工作流编码不能为空")
    private String step2TaskName;

    /**
     * step2 目标站点名：AGV 移动到达的目的地，须与 AGV 地图中的站点名称完全一致。
     * <p>示例：{@code WAREHOUSE}、{@code HCL_STATION}、{@code STX_STATION}。
     */
    @Schema(description = "step2目标站点名（须与AGV地图站点名称完全一致）",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "WAREHOUSE")
    @NotBlank(message = "step2目标站点名不能为空")
    private String step2StationName;

    /**
     * step3 工作流编码：AGV 将物料卸载到目标位的动作编码。
     * <p>取值参考 AGV 工作流词典：
     * <ul>
     *   <li>{@code AGV_HCL} - AGV给后处理工站上料（孔位 1~16）</li>
     *   <li>{@code AGV_STX} - AGV给手套箱工站上料（孔位 1~8）</li>
     *   <li>{@code AGV_FX}  - AGV给分析工站上料（孔位 1~3）</li>
     *   <li>{@code AGV_CC}  - AGV给仓储工站上料（孔位 1~2）</li>
     * </ul>
     */
    @Schema(description = "step3工作流编码：AGV卸载到目标位（参考AGV工作流词典）",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "AGV_CC")
    @NotBlank(message = "step3工作流编码不能为空")
    private String step3TaskName;

    /**
     * 是否启用，默认 {@code true}。
     * <p>{@code false} 可临时停用路由而不删除配置。
     */
    @Schema(description = "是否启用，默认true；false时补料/下料服务无法使用该路由",
            example = "true")
    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

    /** 备注，描述路由用途或注意事项。 */
    @Schema(description = "备注",
            example = "后处理站到仓储的标准下料路由")
    private String remark;
}
