package com.librax.lab.module.flow.engine.execution.callback;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import com.librax.lab.module.flow.api.enums.WaitingForEnum;
import com.librax.lab.module.flow.api.resource.ResourcePool;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.dataobject.stepexecution.StepExecutionDO;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import com.librax.lab.module.flow.dal.mysql.stepexecution.StepExecutionMapper;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.flow.engine.execution.scheduler.DagScheduler;
import com.librax.lab.module.flow.engine.execution.scheduler.StepSubmitter;
import com.librax.lab.module.flow.enums.ExecutionStatusEnum;
import com.librax.lab.module.flow.enums.StepStatusEnum;
import com.librax.lab.module.flow.api.callback.CallbackResult;
import com.librax.lab.module.flow.service.pipelineexecution.PipelineExecutionService;
import com.librax.lab.module.infra.mdc.ExecutionMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static cn.hutool.core.map.MapUtil.getInt;

@Slf4j
@Service
@RequiredArgsConstructor
public class StepCallbackService implements StepCallbackSpi {

    private final PipelineExecutionMapper executionMapper;
    private final StepExecutionMapper stepMapper;
    private final DagScheduler dagScheduler;
    private final ResourcePool resourcePool;
    private final PipelineExecutionService executionService;

    /**
     * 外部回调推进步骤
     * <p>
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
    @Override
    public CallbackResult callback(String executionId,
                                   String nodeId,
                                   String callbackToken,
                                   boolean success,
                                   Map<String, Object> outputs,
                                   String errorCode,
                                   String errorMsg) {
        ExecutionMdc.set(executionId, nodeId, 0);
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

        if (!success && "UNIT_NOT_QUALIFIED".equals(errorCode)) {
            // 子流程不合格，检查是否需要继续循环
            StepResult loopResult = handleUnitLoop(stepDO, outputs, errorMsg);
            if (loopResult != null) {
                // 已经发起新的子流程，不走普通失败处理，直接返回
                return CallbackResult.ok();
            }
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

        // 释放资源(DIRECT 路径,以及 QUEUED 路径走事件桥过来时的兜底)
        String holderKey = StepSubmitter.buildHolderKey(executionId, nodeId, stepDO.getAttempt());
        int released = resourcePool.releaseByHolder(holderKey);
        if (released > 0) {
            log.info("[StepCallbackService] 释放资源 holder={} count={}", holderKey, released);
        }

        return CallbackResult.ok();
    }


    /**
     * 通过 callbackToken 直接回调（子流程唤醒父流程用）
     * 不需要知道父流程的 executionId 和 nodeId，token 唯一定位
     */
    public CallbackResult callbackByToken(String callbackToken,
                                                                                  boolean success,
                                                                                  Map<String, Object> outputs,
                                                                                  String errorCode,
                                                                                  String errorMsg) {
        // 通过 token 查找 WAITING 状态的步骤
        StepExecutionDO stepDO = stepMapper.selectByCallbackToken(callbackToken);
        if (stepDO == null) {
            log.warn("[StepCallback] token 对应步骤不存在或已完成 token={}", callbackToken);
            return CallbackResult.fail("TOKEN_NOT_FOUND", "token不存在或步骤已完成");
        }

        return callback(
                stepDO.getExecutionId(),
                stepDO.getNodeId(),
                callbackToken,
                success,
                outputs,
                errorCode,
                errorMsg);
    }

    /**
     * 处理执行单元循环
     * 如果步骤配置了 unitPipelineKey 且还有重试次数，发起新一轮子流程
     * @return 非 null 表示已处理循环，null 表示按普通失败处理
     */
    private StepResult handleUnitLoop(StepExecutionDO stepDO,
                                      Map<String, Object> outputs,
                                      String errorMsg) {
        // 从步骤的 inputSnapshot 里取执行单元配置
        if (stepDO.getInputSnapshot() == null) return null;

        Map<String, Object> snapshot = JSON.parseObject(
                stepDO.getInputSnapshot(), Map.class);

        String unitPipelineKey = (String) snapshot.get("unitPipelineKey");
        if (unitPipelineKey == null) return null;  // 不是执行单元节点

        int maxRetry     = getInt(snapshot, "maxRetry",     3);
        int currentRetry = getInt(snapshot, "currentRetry", 0);
        int nextRetry    = currentRetry + 1;

        if (nextRetry >= maxRetry) {
            // 超过最大次数，走普通失败
            log.warn("[StepCallback] 执行单元超过最大循环次数 nodeId={} retry={}/{}",
                    stepDO.getNodeId(), nextRetry, maxRetry);
            return null;
        }

        // 还有重试次数，生成新 token，启动新一轮子流程
        String newToken = UUID.randomUUID().toString().replace("-", "");

        // 更新步骤的 callbackToken（新子流程用新 token 回调）
        stepMapper.updateCallbackToken(
                stepDO.getExecutionId(), stepDO.getNodeId(),
                stepDO.getAttempt(), newToken);

        // 把 currentRetry 更新到下一轮
        Map<String, Object> nextParams = new HashMap<>(snapshot);
        nextParams.put("currentRetry",     nextRetry);
        nextParams.put("parentCallbackToken", newToken);

        executionService.startChild(
                unitPipelineKey,
                getInt(snapshot, "unitPipelineVersion"),
                stepDO.getExecutionId(),
                newToken,
                nextParams);

        log.info("[StepCallback] 执行单元发起第{}轮 nodeId={} newToken={}",
                nextRetry + 1, stepDO.getNodeId(), newToken);

        return StepResult.waiting(WaitingForEnum.CHILD_EXECUTION, Map.of(
                "currentRetry", nextRetry));
    }
}