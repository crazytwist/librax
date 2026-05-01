package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知步骤执行器
 * <p>
 * 对应 pd_step_definition.bean_name = 'notifyBean'
 * method_name = 'sendPass' 或 'sendFail' 决定通知类型
 */
@Slf4j
@Component("notifyBean")
public class NotifyBean implements StepExecutor {

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.COMPUTE;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        Object scoreRaw = ctx.getInputParams().get("score");
        int score = scoreRaw != null ? ((Number) scoreRaw).intValue() : 0;
        String method = ctx.getMethodName();

        if ("sendPass".equals(method)) {
            log.info("[Notify] 水质合格通知 score={}", score);
            // TODO: 对接真实通知服务（短信 / 钉钉 / 邮件）
        } else {
            log.warn("[Notify] 水质不合格告警 score={}", score);
            // TODO: 对接告警服务
        }

        return StepResult.ok(Map.of(
                "notified", true,
                "notifiedAt", LocalDateTime.now().toString()
        ));
    }
}