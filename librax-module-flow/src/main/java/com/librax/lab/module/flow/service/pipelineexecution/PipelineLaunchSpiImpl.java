package com.librax.lab.module.flow.service.pipelineexecution;

import com.librax.lab.module.flow.api.pipeline.PipelineLaunchSpi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * {@link PipelineLaunchSpi} 的默认实现，委托给 {@link PipelineExecutionService}。
 */
@Service
@RequiredArgsConstructor
public class PipelineLaunchSpiImpl implements PipelineLaunchSpi {

    private final PipelineExecutionService executionService;

    @Override
    public String launch(String pipelineKey, Map<String, Object> inputParams) {
        return executionService.start(pipelineKey, null, inputParams, "MANUAL", "system", null);
    }

    @Override
    public String launchDetached(String pipelineKey, Map<String, Object> inputParams) {
        return executionService.start(pipelineKey, null, inputParams, "EVENT", "system", null);
    }
}
