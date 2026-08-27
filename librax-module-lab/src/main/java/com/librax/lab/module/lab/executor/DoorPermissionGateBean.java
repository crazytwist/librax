package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.enums.WaitingForEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 等待机台"可以开门"确认信号的纯 DAG 层网关节点。
 *
 * <p>跟 {@link DoorGateBean} 配套：排在 {@code s_door_open} 之前，不涉及 AGV 的
 * waitSignal/startAgain 联动，纯粹是"卡在这里，等业务侧调用推进"，业务侧
 * （librax-dp 新增的 Controller）收到机台的开门许可信号后直接调用
 * {@code StepCallbackSpi.callback(executionId, "s_door_permission_gate", null, true, ...)}
 * 推进本节点，DAG 才会继续往下走到 {@code s_door_open}。
 *
 * <p>输入参数（{@code ctx.getInputParams()}）：
 * <pre>
 *   doorPermissionGateRequired   是否需要等机台许可才能开门，布尔（字符串"true"/"false"亦可）。
 *                                业务约定：同一补料/上料单第一车=false（AGV/开门不用等），
 *                                第二车起=true。为 false 时本节点立即通过。
 * </pre>
 */
@Slf4j
@Component("doorPermissionGateBean")
public class DoorPermissionGateBean implements StepExecutor {

    @Override
    public StepTypeEnum supportType() {
        return null; // 仅按 beanName 路由
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        Map<String, Object> inputParams = ctx.getInputParams();
        if (!bool(inputParams.get("doorPermissionGateRequired"))) {
            log.info("[DoorPermissionGate] 本车不需要等机台开门许可 executionId={} nodeId={}",
                    ctx.getExecutionId(), ctx.getNodeId());
            return StepResult.ok(Map.of("gated", false));
        }
        log.info("[DoorPermissionGate] 挂起，等机台开门许可信号推进 executionId={} nodeId={}",
                ctx.getExecutionId(), ctx.getNodeId());
        return StepResult.waiting(WaitingForEnum.EXTERNAL_EVENT, Map.of("waitReason", "等待机台开门许可"));
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean b) return b;
        return value != null && "true".equalsIgnoreCase(String.valueOf(value));
    }
}
