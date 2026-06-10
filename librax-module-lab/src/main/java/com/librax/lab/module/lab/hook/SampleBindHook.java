package com.librax.lab.module.lab.hook;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.librax.lab.module.flow.api.PipelineStartHook;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleInfoDO;
import com.librax.lab.module.lab.dal.mysql.sample.SampleInfoMapper;
import com.librax.lab.module.lab.service.sample.SampleLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 样本预绑定钩子
 *
 * <p>在流程启动事务内完成：
 * <ol>
 *   <li>样本进入流程（状态 → LOADED，绑定 current_execution_id）</li>
 *   <li>预绑定所有 INSTRUMENT 步骤（创建 lab_sample_step 记录）</li>
 *   <li>将样本 experiment_params 注入 inputParams，步骤可通过 {@code ${input.xxx}} 引用</li>
 * </ol>
 *
 * <p>注入优先级：调用方显式传入的参数 > 样本 experiment_params
 * （用 putIfAbsent 保证调用方参数不被覆盖）
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class SampleBindHook implements PipelineStartHook {

    private final SampleLifecycleService sampleLifecycleService;
    private final SampleInfoMapper       sampleInfoMapper;

    @Override
    public void beforeSchedule(String executionId, String pipelineKey,
                               int version, Map<String, Object> inputParams) {
        List<String> sampleIds = resolveSampleIds(inputParams);
        if (sampleIds.isEmpty()) return;

        // 1. 样本进入流程 + 预绑定步骤
        for (String sampleId : sampleIds) {
            sampleLifecycleService.onSampleLoaded(sampleId, executionId);
        }
        sampleLifecycleService.preBindSteps(sampleIds, executionId, pipelineKey, version);

        // 2. 注入样本实验参数到 inputParams（基础层，调用方传入的参数优先）
        for (String sampleId : sampleIds) {
            injectExperimentParams(sampleId, inputParams);
        }
    }

    /**
     * 将样本 experiment_params 合并到 inputParams
     *
     * <p>多样本场景下，若多个样本 experiment_params 存在同名 key，后者覆盖前者。
     * 实际上批量场景通常所有样本共享同一套参数 key，值不同时应使用单样本子流程（UNIT_LAUNCHER）。
     */
    private void injectExperimentParams(String sampleId, Map<String, Object> inputParams) {
        SampleInfoDO sample = sampleInfoMapper.selectBySampleId(sampleId);
        if (sample == null || sample.getExperimentParams() == null
                || sample.getExperimentParams().isBlank()) {
            return;
        }
        try {
            Map<String, Object> expParams = JSON.parseObject(
                    sample.getExperimentParams(),
                    new TypeReference<Map<String, Object>>() {});
            if (expParams == null || expParams.isEmpty()) return;

            // putIfAbsent：调用方显式传入的参数不被覆盖
            expParams.forEach(inputParams::putIfAbsent);
            log.debug("[SampleBind] 实验参数已注入 sampleId={} keys={}",
                    sampleId, expParams.keySet());
        } catch (Exception e) {
            log.warn("[SampleBind] experiment_params 解析失败，跳过注入 sampleId={}", sampleId, e);
        }
    }

    private List<String> resolveSampleIds(Map<String, Object> params) {
        if (params == null) return List.of();
        Object ids = params.get("sampleIds");
        if (ids instanceof List) {
            return ((List<?>) ids).stream()
                    .map(Object::toString).collect(Collectors.toList());
        }
        Object id = params.get("sampleId");
        return id != null ? List.of(id.toString()) : List.of();
    }
}
