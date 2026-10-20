package com.librax.lab.module.resource.service.agvload;

import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import com.librax.lab.module.flow.api.device.DeviceCommandSpi;
import com.librax.lab.module.flow.api.material.MaterialLocationSpi;
import com.librax.lab.module.flow.api.pipeline.PipelineExecutionSpi;
import com.librax.lab.module.resource.dal.dataobject.agvload.AgvLoadPlanDO;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvLoadItemMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvLoadPlanMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvOrchestrateQueueMapper;
import com.librax.lab.module.resource.dal.mysql.agvload.AgvTaskQueueMapper;
import com.librax.lab.module.resource.dal.mysql.slotinfo.SlotInfoMapper;
import com.librax.lab.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 模拟生产日志中的 UNLOAD waitSignal：current_agv_task_id 索引查不到，但 plan 行已就绪。
 */
class AgvLoadPlanWaitSignalSimulationTest extends BaseMockitoUnitTest {

    private static final String PLAN_TASK_ID = "e90a45fada1b4c7e8d7f16fa0594c80f";
    private static final String AGV_TASK_ID = PLAN_TASK_ID + "-UNLOAD";
    private static final String NODE = "orch-node-1";

    @InjectMocks
    private AgvLoadPlanServiceImpl service;

    @Mock
    private AgvLoadPlanMapper planMapper;
    @Mock
    private AgvLoadItemMapper itemMapper;
    @Mock
    private AgvTaskQueueMapper queueMapper;
    @Mock
    private AgvOrchestrateQueueMapper orchestrateQueueMapper;
    @Mock
    private SlotInfoMapper slotMapper;
    @Mock
    private DeviceCommandSpi deviceCommandSpi;
    @Mock
    private StepCallbackSpi stepCallbackSpi;
    @Mock
    private MaterialLocationSpi materialLocationSpi;
    @Mock
    private AgvQueueService agvQueueService;
    @Mock
    private PipelineExecutionSpi executionSpi;

    private AgvLoadPlanDO readyPlan;

    @BeforeEach
    void setUpPlan() {
        readyPlan = AgvLoadPlanDO.builder().taskId(PLAN_TASK_ID).orchestrationNodeId(NODE)
                .currentAgvTaskId(AGV_TASK_ID).currentAgvOperation("UNLOAD").status("WAIT_AGV_UNLOAD")
                .deviceId("AGV-01").build();
    }

    @Test
    void productionScenario_indexMiss_planTaskIdFallback_consumedByCoordinator() {
        when(planMapper.selectByCurrentAgvTaskId(AGV_TASK_ID)).thenReturn(null);
        when(planMapper.selectByTaskId(PLAN_TASK_ID)).thenReturn(readyPlan);
        when(planMapper.selectByCurrentAgvTaskIdForUpdate(AGV_TASK_ID)).thenReturn(readyPlan);

        try (MockedStatic<TransactionSynchronizationManager> tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            ArgumentCaptor<TransactionSynchronization> sync = ArgumentCaptor.forClass(TransactionSynchronization.class);
            tx.when(() -> TransactionSynchronizationManager.registerSynchronization(sync.capture()))
                    .thenAnswer(inv -> null);

            assertTrue(service.handle(AGV_TASK_ID, "LM3"));
        }

        assertEquals("WAIT_SITE_ACTION", readyPlan.getStatus());
        assertEquals("LM3", readyPlan.getLastAgvStation());
        verify(planMapper).updateById(readyPlan);
        verify(stepCallbackSpi, never()).callback(any(), any(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void raceThenIndexCatchUp_succeedsWithinRetryWindow() {
        when(planMapper.selectByCurrentAgvTaskId(AGV_TASK_ID)).thenReturn(null, null, readyPlan);
        when(planMapper.selectByTaskId(PLAN_TASK_ID)).thenReturn(readyPlan);
        when(planMapper.selectByCurrentAgvTaskIdForUpdate(AGV_TASK_ID)).thenReturn(readyPlan);

        try (MockedStatic<TransactionSynchronizationManager> tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
            tx.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).thenAnswer(inv -> null);

            assertTrue(service.handle(AGV_TASK_ID, "LM3"));
        }
        verify(planMapper, atLeast(2)).selectByCurrentAgvTaskId(AGV_TASK_ID);
    }

    @Test
    void loadingMode_plainTaskId_notDelayedByRetryLoop() {
        when(planMapper.selectByCurrentAgvTaskId(PLAN_TASK_ID)).thenReturn(null);
        when(planMapper.selectByTaskId(PLAN_TASK_ID)).thenReturn(null);

        long start = System.nanoTime();
        assertFalse(service.handle(PLAN_TASK_ID, "AP1"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs < 200, "无编排后缀时不应进入 5×100ms 重试，实际 ms=" + elapsedMs);
        verify(planMapper, times(1)).selectByCurrentAgvTaskId(PLAN_TASK_ID);
        verify(planMapper, times(1)).selectByTaskId(PLAN_TASK_ID);
    }
}
