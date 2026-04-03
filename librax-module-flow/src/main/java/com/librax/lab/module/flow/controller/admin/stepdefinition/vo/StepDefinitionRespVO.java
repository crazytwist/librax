package com.librax.lab.module.flow.controller.admin.stepdefinition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 步骤定义表，可复用的步骤组件库 [pd_] Response VO")
@Data
@ExcelIgnoreUnannotated
public class StepDefinitionRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "26550")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "步骤唯一标识，小写_下划线，如 ph_measure", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("步骤唯一标识，小写_下划线，如 ph_measure")
    private String stepKey;

    @Schema(description = "版本号，同 key 下从 1 递增", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("版本号，同 key 下从 1 递增")
    private Integer version;

    @Schema(description = "步骤显示名称，如 PH检测", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("步骤显示名称，如 PH检测")
    private String name;

    @Schema(description = "步骤说明", example = "你猜")
    @ExcelProperty("步骤说明")
    private String description;

    @Schema(description = "INSTRUMENT 仪器 | COMPUTE 计算 | CONDITION 条件 | WAIT 等待 | NOTIFY 通知", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("INSTRUMENT 仪器 | COMPUTE 计算 | CONDITION 条件 | WAIT 等待 | NOTIFY 通知")
    private String stepType;

    @Schema(description = "设备类型，如 PH_METER，step_type=INSTRUMENT 时必填", example = "1")
    @ExcelProperty("设备类型，如 PH_METER，step_type=INSTRUMENT 时必填")
    private String deviceType;

    @Schema(description = "默认设备指令，如 MEASURE，可被 pd_pipeline_step 覆盖")
    @ExcelProperty("默认设备指令，如 MEASURE，可被 pd_pipeline_step 覆盖")
    private String command;

    @Schema(description = "BEAN | LITEFLOW，step_type=COMPUTE 时必填")
    @ExcelProperty("BEAN | LITEFLOW，step_type=COMPUTE 时必填")
    private String executor;

    @Schema(description = "executor=BEAN 时的 Spring Bean 名称", example = "芋艿")
    @ExcelProperty("executor=BEAN 时的 Spring Bean 名称")
    private String beanName;

    @Schema(description = "executor=BEAN 时的方法名", example = "芋艿")
    @ExcelProperty("executor=BEAN 时的方法名")
    private String methodName;

    @Schema(description = "executor=LITEFLOW 时的 Chain ID", example = "25931")
    @ExcelProperty("executor=LITEFLOW 时的 Chain ID")
    private String chainId;

    @Schema(description = "默认参数值，被 pd_pipeline_step.params_override 同名字段覆盖")
    @ExcelProperty("默认参数值，被 pd_pipeline_step.params_override 同名字段覆盖")
    private String defaultParams;

    @Schema(description = "参数 schema，供前端渲染表单和后端入参校验")
    @ExcelProperty("参数 schema，供前端渲染表单和后端入参校验")
    private String paramsSchema;

    @Schema(description = "步骤输出字段声明")
    @ExcelProperty("步骤输出字段声明")
    private String outputFields;

    @Schema(description = "默认超时时间(ms)")
    @ExcelProperty("默认超时时间(ms)")
    private Long defaultTimeoutMs;

    @Schema(description = "默认最大重试次数")
    @ExcelProperty("默认最大重试次数")
    private Integer defaultMaxAttempts;

    @Schema(description = "默认重试退避时间(ms)")
    @ExcelProperty("默认重试退避时间(ms)")
    private Long defaultBackoffMs;

    @Schema(description = "默认补偿步骤的 step_key，失败触发补偿时调用")
    @ExcelProperty("默认补偿步骤的 step_key，失败触发补偿时调用")
    private String compensateStepKey;

    @Schema(description = "补偿步骤的默认入参")
    @ExcelProperty("补偿步骤的默认入参")
    private String compensateParams;

    @Schema(description = "是否支持单独运行 1 支持 0 不支持", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("是否支持单独运行 1 支持 0 不支持")
    private Boolean runnableStandalone;

    @Schema(description = "单独运行或测试时的 Mock 输出，跳过真实执行")
    @ExcelProperty("单独运行或测试时的 Mock 输出，跳过真实执行")
    private String mockOutput;

    @Schema(description = "ACTIVE 启用  DISABLED 停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("ACTIVE 启用  DISABLED 停用")
    private String status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}