package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.resource.service.agvload.AgvReturnPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * AGV 下料库位提交 Bean。
 *
 * <p>所有物料已由仓储机械臂确认入库后，执行最终清理：
 * 清理 AGV 槽位占用，将明细状态置为 COMPLETED。
 */
@Slf4j
@Component("agvCommitReturnBean")
@RequiredArgsConstructor
public class AgvCommitReturnBean implements StepExecutor {

    private final AgvReturnPlanService agvReturnPlanService;

    @Override
    public StepTypeEnum supportType() {
        return null; // 仅按 beanName 路由
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StepResult execute(StepDispatchContext ctx) {
        agvReturnPlanService.commitReturn(ctx.getExecutionId());
        log.info("[AgvReturn] 下料提交完成 executionId={}", ctx.getExecutionId());
        return StepResult.ok(Map.of("committed", true, "returnFlow", true));
    }
}
