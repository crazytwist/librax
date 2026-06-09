package com.librax.lab.module.lab.flow;

import com.librax.lab.module.flow.api.PipelineStartHook;
import com.librax.lab.module.lab.dal.mysql.sample.SampleInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 流程启动时样本绑定钩子
 *
 * <p>职责：当流程 inputParams 中包含 sampleId 时，
 * 更新 lab_sample_info.current_execution_id 指向本次执行，实现 sample→execution 双向关联。
 *
 * <p>触发时机：{@link PipelineStartHook#beforeSchedule} 在事务内、调度器触发前执行。
 *
 * <p>幂等性：若 sampleId 对应的记录不存在，静默跳过（update rows=0）。
 *
 * <p>使用方式：调用 start() 或 startChild() 时，在 inputParams 中传入 sampleId：
 * <pre>{@code
 *   executionService.start("water_quality_check", null,
 *       Map.of("sampleId", "SMPL-20260501-001", "targetPh", 7.2),
 *       "MANUAL", userId, zoneCode);
 * }</pre>
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class SampleExecutionBindHook implements PipelineStartHook {

    private final SampleInfoMapper sampleInfoMapper;

    @Override
    public void beforeSchedule(String executionId,
                               String pipelineKey,
                               int version,
                               Map<String, Object> inputParams) {
        if (inputParams == null) return;

        Object sampleIdVal = inputParams.get("sampleId");
        if (sampleIdVal == null) return;

        String sampleId = sampleIdVal.toString();

        // 更新样本的当前执行ID，同时清空 currentNodeId（本次执行刚开始，尚未进入任何步骤）
        int rows = sampleInfoMapper.updateCurrentExecutionId(sampleId, executionId);
        if (rows > 0) {
            log.info("[SampleBind] 样本已绑定执行 sampleId={} executionId={} pipelineKey={}",
                    sampleId, executionId, pipelineKey);
        } else {
            log.warn("[SampleBind] 样本不存在，跳过绑定 sampleId={} executionId={}",
                    sampleId, executionId);
        }
    }
}
