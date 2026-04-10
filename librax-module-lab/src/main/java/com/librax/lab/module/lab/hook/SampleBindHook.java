package com.librax.lab.module.lab.hook;

import com.librax.lab.module.flow.api.PipelineStartHook;
import com.librax.lab.module.lab.service.sample.SampleLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 样本预绑定钩子
 * lab 模块实现 flow-api 里的 SPI，flow 模块通过 Spring 自动发现
 */
@Component
@Order(10)  // 多个钩子时控制顺序
@RequiredArgsConstructor
public class SampleBindHook implements PipelineStartHook {

    private final SampleLifecycleService sampleLifecycleService;

    @Override
    public void beforeSchedule(String executionId, String pipelineKey,
                               int version, Map<String, Object> inputParams) {
        List<String> sampleIds = resolveSampleIds(inputParams);
        if (sampleIds.isEmpty()) return;

        // 样本进入流程 + 预绑定（同步，在 start() 事务内）
        for (String sampleId : sampleIds) {
            sampleLifecycleService.onSampleLoaded(sampleId, executionId);
        }
        sampleLifecycleService.preBindSteps(
                sampleIds, executionId, pipelineKey, version);
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