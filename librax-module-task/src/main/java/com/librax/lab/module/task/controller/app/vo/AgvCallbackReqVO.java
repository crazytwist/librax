package com.librax.lab.module.task.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * AGV 任务回调请求 VO
 * <p>
 * AGV 调度端在任务完成(上料/下料到位)或失败(路径阻塞/AGV 故障)后,
 * 调此接口推进流程。
 * <p>
 * 约定:调度端必须透传发任务时引擎传入的 callbackToken,用于反查任务。
 */
@Schema(description = "AGV 任务回调请求")
@Data
public class AgvCallbackReqVO {

    @Schema(description = "回调令牌,发任务时引擎生成并透传给 AGV 调度端",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "a1b2c3d4e5f6")
    @NotBlank(message = "回调令牌不能为空")
    private String callbackToken;

    @Schema(description = "AGV 调度端的任务号,用于排查问题时反查调度端日志",
            example = "AGV-JOB-20260420-001")
    private String jobId;

    @Schema(description = "执行结果:SUCCESS 成功到位 / FAILED 执行失败",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "SUCCESS")
    @NotBlank(message = "执行结果不能为空")
    private String status;

    @Schema(description = "执行的 AGV 编号,便于审计和故障定位",
            example = "AGV-01")
    private String agvId;

    @Schema(description = "任务产出,success 时写入流程上下文。" +
            "典型字段:toLocation 实际到达位置、finishedAt 完成时间、distance 行驶距离",
            example = "{\"toLocation\":\"WS-A-01\",\"distance\":12.5}")
    private Map<String, Object> outputs;

    @Schema(description = "错误码,status=FAILED 时填写",
            example = "PATH_BLOCKED")
    private String errorCode;

    @Schema(description = "错误信息,status=FAILED 时填写",
            example = "目标路径被占用超过 5 分钟")
    private String errorMsg;
}