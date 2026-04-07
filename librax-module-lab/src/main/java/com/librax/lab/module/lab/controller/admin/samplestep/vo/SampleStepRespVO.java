package com.librax.lab.module.lab.controller.admin.samplestep.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 样本-步骤绑定表，记录样本在每个流程步骤中的处理状态 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SampleStepRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "27896")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "样本ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "31248")
    @ExcelProperty("样本ID")
    private String sampleId;

    @Schema(description = "流程执行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "29482")
    @ExcelProperty("流程执行ID")
    private String executionId;

    @Schema(description = "步骤节点ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "28608")
    @ExcelProperty("步骤节点ID")
    private String nodeId;

    @Schema(description = "对应 pe_step_execution.attempt，重试时同步", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("对应 pe_step_execution.attempt，重试时同步")
    private Integer attempt;

    @Schema(description = "冗余步骤标识，便于按检测类型统计")
    @ExcelProperty("冗余步骤标识，便于按检测类型统计")
    private String stepKey;

    @Schema(description = "冗余步骤类型：INSTRUMENT/COMPUTE等", example = "2")
    @ExcelProperty("冗余步骤类型：INSTRUMENT/COMPUTE等")
    private String stepType;

    @Schema(description = "样本在该步骤中的角色：INPUT待处理 / OUTPUT处理产出 / CONSUMED已消耗", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("样本在该步骤中的角色：INPUT待处理 / OUTPUT处理产出 / CONSUMED已消耗")
    private String role;

    @Schema(description = "绑定方式：AUTO自动绑定 / MANUAL人工绑定 / SCAN扫码绑定", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("绑定方式：AUTO自动绑定 / MANUAL人工绑定 / SCAN扫码绑定")
    private String bindType;

    @Schema(description = "BOUND已绑定 / PROCESSING处理中 / COMPLETED已完成 / FAILED失败 / UNBOUND已解绑", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("BOUND已绑定 / PROCESSING处理中 / COMPLETED已完成 / FAILED失败 / UNBOUND已解绑")
    private String status;

    @Schema(description = "处理该样本的设备ID", example = "29142")
    @ExcelProperty("处理该样本的设备ID")
    private String deviceId;

    @Schema(description = "设备通道号/端口号")
    @ExcelProperty("设备通道号/端口号")
    private String deviceChannel;

    @Schema(description = "在设备/容器中的位置（如微孔板孔位 A1/B2、样品架位置 1-10）")
    @ExcelProperty("在设备/容器中的位置（如微孔板孔位 A1/B2、样品架位置 1-10）")
    private String position;

    @Schema(description = "该样本在该步骤的原始检测结果（冗余存，快速查看）")
    @ExcelProperty("该样本在该步骤的原始检测结果（冗余存，快速查看）")
    private String resultData;

    @Schema(description = "处理顺序号，同一步骤内按此排序，NULL表示批量同时处理不区分顺序")
    @ExcelProperty("处理顺序号，同一步骤内按此排序，NULL表示批量同时处理不区分顺序")
    private Integer seqNo;

    @Schema(description = "绑定时间")
    @ExcelProperty("绑定时间")
    private LocalDateTime boundAt;

    @Schema(description = "开始处理时间")
    @ExcelProperty("开始处理时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成处理时间")
    @ExcelProperty("完成处理时间")
    private LocalDateTime finishedAt;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}