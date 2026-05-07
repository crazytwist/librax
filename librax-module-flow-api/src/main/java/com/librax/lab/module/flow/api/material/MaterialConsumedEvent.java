package com.librax.lab.module.flow.api.material;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 步骤执行成功后发布的物料消耗事件
 * <p>
 * flow 引擎在步骤成功后发布此事件，lab 模块监听并执行：
 * <ol>
 *   <li>库存扣减（current_vol_ul 减少）
 *   <li>消耗记录写入（lab_material_consumption）
 *   <li>库位位置更新（如果有 TRANSFER 操作）
 * </ol>
 */
@Getter
public class MaterialConsumedEvent extends ApplicationEvent {

    private final String executionId;
    private final String nodeId;
    private final int attempt;
    private final Long pipelineStepId;
    private final String zoneCode;
    /** 步骤输出，里面可能含 consumedMaterials 等信息 */
    private final Map<String, Object> outputs;

    public MaterialConsumedEvent(Object source,
                                 String executionId,
                                 String nodeId,
                                 int attempt,
                                 Long pipelineStepId,
                                 String zoneCode,
                                 Map<String, Object> outputs) {
        super(source);
        this.executionId = executionId;
        this.nodeId = nodeId;
        this.attempt = attempt;
        this.pipelineStepId = pipelineStepId;
        this.zoneCode = zoneCode;
        this.outputs = outputs;
    }
}
