package com.librax.lab.module.flow.engine.execution.callback;

import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.engine.execution.scheduler.DagScheduler;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StepCallbackService {

    private final PipelineExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;
    private final DagScheduler dagScheduler;

    /**
     * 外部回调推进步骤
     *
     * 设备回调、人工审批、外部事件都走这个方法
     *
     * @param executionId   执行实例ID
     * @param nodeId        节点ID
     * @param callbackToken 回调令牌（创建等待时生成，必须匹配）
     * @param success       是否成功
     * @param outputs       输出数据
     * @param errorCode     错误码（失败时）
     * @param errorMsg      错误信息（失败时）
     * @return 推进结果
     */
    public CallbackResult callback(String executionId,
                                   String nodeId,
                                   String callbackToken,
                                   boolean success,
                                   Map<String, Object> outputs,
                                   String errorCode,
                                   String errorMsg) {
        // 1. 校验执行实例
        PipelineExecutionDO execution = executionMapper
                .selectByExecutionId(executionId);
        if (execution == null) {
            return CallbackResult.fail("EXECUTION_NOT_FOUND", "执行实例不存在");
        }
        if (!ExecutionStatusEnum.RUNNING.name().equals(execution.getStatus())) {
            return CallbackResult.fail("EXECUTION_NOT_RUNNING",
                    "执行实例不是RUNNING状态，当前: " + execution.getStatus());
        }

        // 2. 校验步骤
        StepExecutionDO stepDO = stepMapper
                .selectLatestAttempt(executionId, nodeId);
        if (stepDO == null) {
            return CallbackResult.fail("STEP_NOT_FOUND", "步骤不存在");
        }
        if (!StepStatusEnum.WAITING.name().equals(stepDO.getStatus())) {
            return CallbackResult.fail("STEP_NOT_WAITING",
                    "步骤不是WAITING状态，当前: " + stepDO.getStatus());
        }

        // 3. 校验回调令牌（防伪造、防重放）
        if (callbackToken != null
                && !callbackToken.equals(stepDO.getCallbackToken())) {
            log.warn("[StepCallback] 令牌不匹配 executionId={} nodeId={} " +
                            "expected={} actual={}",
                    executionId, nodeId,
                    stepDO.getCallbackToken(), callbackToken);
            return CallbackResult.fail("TOKEN_MISMATCH", "回调令牌不匹配");
        }

        // 4. 构建结果
        StepResult stepResult;
        if (success) {
            stepResult = StepResult.ok(outputs != null ? outputs : Map.of());
        } else {
            stepResult = StepResult.fail(
                    errorCode != null ? errorCode : "CALLBACK_FAIL",
                    errorMsg != null ? errorMsg : "外部回调报告失败");
        }

        // 5. 推进流程（复用现有的 onStepComplete 链路）
        log.info("[StepCallback] 推进步骤 executionId={} nodeId={} success={}",
                executionId, nodeId, success);

        dagScheduler.onStepComplete(
                executionId,
                execution.getPipelineKey(),
                execution.getPipelineVersion(),
                nodeId,
                stepDO.getAttempt(),
                stepResult);

        return CallbackResult.ok();
    }
}