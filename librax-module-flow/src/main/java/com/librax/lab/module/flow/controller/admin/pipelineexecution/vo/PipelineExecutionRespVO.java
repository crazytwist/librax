package com.librax.lab.module.flow.controller.admin.pipelineexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 流程执行实例，支持完整流程、节点单独运行、补偿执行 Response VO")
@Data
@ExcelIgnoreUnannotated
public class PipelineExecutionRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "19975")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "执行唯一业务 ID，UUID，对外暴露", requiredMode = Schema.RequiredMode.REQUIRED, example = "17617")
    @ExcelProperty("执行唯一业务 ID，UUID，对外暴露")
    private String executionId;

    @Schema(description = "流程标识，冗余存便于查询", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("流程标识，冗余存便于查询")
    private String pipelineKey;

    @Schema(description = "执行时绑定的定义版本，启动后锁定不变", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("执行时绑定的定义版本，启动后锁定不变")
    private Integer pipelineVersion;

    @Schema(description = "PENDING | RUNNING | PAUSED | SUCCESS | FAILED | CANCELLED | COMPENSATING | COMPENSATED", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("PENDING | RUNNING | PAUSED | SUCCESS | FAILED | CANCELLED | COMPENSATING | COMPENSATED")
    private String status;

    @Schema(description = "MANUAL | SCHEDULE | EVENT | RETRY | STANDALONE", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("MANUAL | SCHEDULE | EVENT | RETRY | STANDALONE")
    private String triggerType;

    @Schema(description = "触发人 ID 或触发源标识")
    @ExcelProperty("触发人 ID 或触发源标识")
    private String triggeredBy;

    @Schema(description = "执行所在区域，关联 lab_zone_slot")
    @ExcelProperty("执行所在区域，关联 lab_zone_slot")
    private String zoneCode;

    @Schema(description = "外部传入初始参数，节点可通过 ${input.xxx} 引用")
    @ExcelProperty("外部传入初始参数，节点可通过 ${input.xxx} 引用")
    private String inputParams;

    @Schema(description = "父执行 ID，trigger_type=STANDALONE 时填写", example = "22540")
    @ExcelProperty("父执行 ID，trigger_type=STANDALONE 时填写")
    private String parentExecutionId;

    @Schema(description = "单独运行的节点 ID，trigger_type=STANDALONE 时填写", example = "13673")
    @ExcelProperty("单独运行的节点 ID，trigger_type=STANDALONE 时填写")
    private String standaloneNodeId;

    @Schema(description = "被补偿的原始执行 ID，补偿执行时填写", example = "9670")
    @ExcelProperty("被补偿的原始执行 ID，补偿执行时填写")
    private String originExecutionId;

    @Schema(description = "首个节点开始执行时填写")
    @ExcelProperty("首个节点开始执行时填写")
    private LocalDateTime startedAt;

    @Schema(description = "流程进入终态时填写")
    @ExcelProperty("流程进入终态时填写")
    private LocalDateTime finishedAt;

    @Schema(description = "总耗时(ms)")
    @ExcelProperty("总耗时(ms)")
    private Long totalMs;

    @Schema(description = "关键路径 node_id 列表，流程结束后异步计算写入")
    @ExcelProperty("关键路径 node_id 列表，流程结束后异步计算写入")
    private String criticalPath;

    @Schema(description = "乐观锁，状态流转时 WHERE row_version=#{v} 防并发", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("乐观锁，状态流转时 WHERE row_version=#{v} 防并发")
    private Integer rowVersion;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}