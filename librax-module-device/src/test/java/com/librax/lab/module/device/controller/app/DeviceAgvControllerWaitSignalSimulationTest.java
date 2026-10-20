package com.librax.lab.module.device.controller.app;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.device.api.AgvWaitSignalHandler;
import com.librax.lab.module.device.api.AgvTaskStateHandler;
import com.librax.lab.module.device.api.AgvResumeHandler;
import com.librax.lab.module.device.callback.DeviceCallbackHandler;
import com.librax.lab.module.device.controller.app.vo.AgvWaitSignalReqVO;
import com.librax.lab.module.device.gateway.DeviceStateCache;
import com.librax.lab.module.flow.api.callback.CallbackResult;
import com.librax.lab.module.flow.api.callback.StepCallbackSpi;
import com.librax.lab.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeviceAgvControllerWaitSignalSimulationTest extends BaseMockitoUnitTest {

    private static final String UNLOAD_TASK = "e90a45fada1b4c7e8d7f16fa0594c80f-UNLOAD";
    private static final String FLOW_EXEC = "e90a45fada1b4c7e8d7f16fa0594c80f";

    private DeviceAgvController controller;

    @Mock
    private DeviceCallbackHandler deviceCallbackHandler;
    @Mock
    private DeviceStateCache deviceStateCache;
    @Mock
    private StepCallbackSpi stepCallbackSpi;
    @Mock
    private AgvWaitSignalHandler waitSignalHandler;
    @Mock
    private AgvTaskStateHandler taskStateHandler;
    @Mock
    private AgvResumeHandler resumeHandler;

    @BeforeEach
    void setUpController() {
        controller = new DeviceAgvController(deviceCallbackHandler, deviceStateCache, stepCallbackSpi,
                List.of(waitSignalHandler), List.of(taskStateHandler), List.of(resumeHandler));
    }

    @Test
    void orchestrationSubtask_handlerMiss_returns503_notExecutionNotFound() {
        when(waitSignalHandler.handle(eq(UNLOAD_TASK), any())).thenReturn(false);

        AgvWaitSignalReqVO req = new AgvWaitSignalReqVO();
        req.setTaskId(UNLOAD_TASK);
        req.setAgvStation("LM3");

        CommonResult<Boolean> result = controller.waitSignal(req);

        assertEquals(503, result.getCode());
        assertTrue(result.getMsg().contains("请稍后重试"));
        verify(stepCallbackSpi, never()).callback(any(), any(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void pureFlowExecutionId_stillUsesStepCallback() {
        when(waitSignalHandler.handle(eq(FLOW_EXEC), any())).thenReturn(false);
        when(stepCallbackSpi.callback(eq(FLOW_EXEC), any(), any(), eq(true), any(), any(), any()))
                .thenReturn(CallbackResult.ok());

        AgvWaitSignalReqVO req = new AgvWaitSignalReqVO();
        req.setTaskId(FLOW_EXEC);
        req.setAgvStation("ANALYZER-01");

        CommonResult<Boolean> result = controller.waitSignal(req);

        assertTrue(result.isSuccess());
        verify(stepCallbackSpi).callback(eq(FLOW_EXEC), any(), any(), eq(true), any(), any(), any());
    }
}
