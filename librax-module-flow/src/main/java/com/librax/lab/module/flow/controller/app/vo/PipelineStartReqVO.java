package com.librax.lab.module.flow.controller.app.vo;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class PipelineStartReqVO {

    @NotBlank(message = "流程标识不能为空")
    private String pipelineKey;

    @NotNull(message = "版本号不能为空")
    private Integer pipelineVersion;

    /** 业务入参，如 sampleId、batchNo 等 */
    private Map<String, Object> inputParams;

    private String triggerType;
    private String triggeredBy;

    /**
     * 执行区域，可为 null（不限区域，资源申请时从公共池取）
     * 示例：ZONE-A / ZONE-B
     */
    private String zoneCode;
}