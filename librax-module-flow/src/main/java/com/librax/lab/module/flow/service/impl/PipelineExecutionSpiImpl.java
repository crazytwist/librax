package com.librax.lab.module.flow.service.impl;

import com.librax.lab.module.flow.api.pipeline.PipelineExecutionSpi;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineExecutionSpiImpl implements PipelineExecutionSpi {

    private final PipelineExecutionMapper executionMapper;

    @Override
    public String selectParentExecutionId(String executionId) {
        return executionMapper.selectParentExecutionId(executionId);
    }
}
