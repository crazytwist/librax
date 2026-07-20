package com.librax.lab.module.resource.dal.dataobject.agvroute;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * AGV 路由配置数据对象。
 *
 * <p>每条记录描述一条完整的 AGV 搬运路线，包含三步动作的工作流编码：
 * <ul>
 *   <li><b>step1</b>：物料装载到 AGV（如 站位→AGV、仓储中转→AGV）</li>
 *   <li><b>step2</b>：AGV 整体移动到目标站点</li>
 *   <li><b>step3</b>：AGV 将物料卸载到目标位（如 AGV→站位、AGV→仓储中转）</li>
 * </ul>
 *
 * <p>工作流编码（taskName）取值参见 AGV 工作流词典：
 * <pre>
 *   HCL_AGV  - 后处理工站下料到AGV   （step1，下料）    孔位编号 1~16
 *   AGV_HCL  - AGV给后处理工站上料   （step3，补料）    孔位编号 1~16
 *   STX_AGV  - 手套箱工站下料到AGV   （step1，下料）    孔位编号 1~8
 *   AGV_STX  - AGV给手套箱工站上料   （step3，补料）    孔位编号 1~8
 *   FX_AGV   - 分析工站下料到AGV     （step1，下料）    孔位编号 1~3
 *   AGV_FX   - AGV给分析工站上料     （step3，补料）    孔位编号 1~3
 *   CC_AGV   - 仓储工站下料到AGV     （step1，补料）    孔位编号 1~2
 *   AGV_CC   - AGV给仓储工站上料     （step3，下料）    孔位编号 1~2
 *   AgvMove  - AGV整体移动           （step2，固定值）
 * </pre>
 *
 * <p>{@code creator}、{@code updater}、{@code createTime}、{@code updateTime}、{@code deleted}
 * 五个审计字段继承自 {@link BaseDO}，由框架自动填充，无需手动赋值。
 */
@TableName("lab_agv_route_config")
@KeySequence("lab_agv_route_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgvRouteConfigDO extends BaseDO {

    /** 主键。 */
    @TableId
    private Long id;

    /**
     * 路由唯一编码，业务查询键。
     * <p>建议命名规则：{起始站}_TO_{目标站}，如 {@code HCL_TO_WAREHOUSE}。
     */
    private String routeCode;

    /** 路由名称，可读描述，如"后处理工站 → 仓储"。 */
    private String routeName;

    /**
     * 起始站点编码，对应 AGV 地图区域标识。
     * <p>示例：{@code HCL}（后处理）、{@code WAREHOUSE}（仓储）、{@code STX}（手套箱）、{@code FX}（分析站）。
     */
    private String sourceStationCode;

    /**
     * 目标站点编码，对应 AGV 地图区域标识。
     * <p>示例：{@code WAREHOUSE}（仓储）、{@code HCL}（后处理）。
     */
    private String destStationCode;

    /**
     * AGV 任务类型，透传给 AGV 设备，由厂商定义。
     * <p>通常为 {@code "1"}，无特殊需求时保持默认。
     */
    private String taskType;

    /**
     * AGV 托板类型，透传给 AGV 设备，由厂商定义。
     * <p>通常为 {@code "1"}，无特殊需求时保持默认。
     */
    private String plateType;

    /**
     * step1 工作流编码：物料装载到 AGV 的动作。
     * <p>示例：{@code HCL_AGV}（站位下料到AGV）、{@code CC_AGV}（仓储中转到AGV）。
     */
    private String step1TaskName;

    /**
     * step2 工作流编码：AGV 整体移动的动作，通常固定为 {@code AgvMove}。
     */
    private String step2TaskName;

    /**
     * step2 目标站点名：AGV 移动到达的目的地，须与 AGV 地图中的站点名称完全一致。
     * <p>示例：{@code WAREHOUSE}、{@code HCL_STATION}。
     */
    private String step2StationName;

    /**
     * step3 工作流编码：AGV 将物料卸载到目标位的动作。
     * <p>示例：{@code AGV_CC}（AGV卸到仓储中转）、{@code AGV_HCL}（AGV卸到站位）。
     */
    private String step3TaskName;

    /**
     * 是否启用。
     * <p>{@code false} 时路由不会被补料/下料服务选取。
     */
    private Boolean enabled;

    /** 备注，描述路由用途或注意事项。 */
    private String remark;
}
