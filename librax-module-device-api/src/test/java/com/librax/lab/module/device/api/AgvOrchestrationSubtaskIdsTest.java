package com.librax.lab.module.device.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgvOrchestrationSubtaskIdsTest {

    private static final String PLAN = "e90a45fada1b4c7e8d7f16fa0594c80f";

    @Test
    void strip_loadPlanUnloadMoveSuffixes() {
        assertEquals(PLAN, AgvOrchestrationSubtaskIds.stripToPlanTaskId(PLAN + "-UNLOAD"));
        assertEquals(PLAN, AgvOrchestrationSubtaskIds.stripToPlanTaskId(PLAN + "-MOVE"));
        assertEquals(PLAN, AgvOrchestrationSubtaskIds.stripToPlanTaskId(PLAN + "-PREPOSITION"));
        assertEquals(PLAN, AgvOrchestrationSubtaskIds.stripToPlanTaskId(PLAN + "-LOAD-3"));
        assertTrue(AgvOrchestrationSubtaskIds.isOrchestrationSubtaskAgvTaskId(PLAN + "-UNLOAD"));
    }

    @Test
    void strip_returnUnloadItemSuffix() {
        String agvTaskId = PLAN + "-UNLOAD-2-I5";
        assertEquals(PLAN, AgvOrchestrationSubtaskIds.stripToPlanTaskId(agvTaskId));
        assertTrue(AgvOrchestrationSubtaskIds.isOrchestrationSubtaskAgvTaskId(agvTaskId));
    }

    @Test
    void pureExecutionId_notOrchestrationSubtask() {
        assertNull(AgvOrchestrationSubtaskIds.stripToPlanTaskId(PLAN));
        assertFalse(AgvOrchestrationSubtaskIds.isOrchestrationSubtaskAgvTaskId(PLAN));
    }

    @Test
    void productionBugTaskId_recognized() {
        String unload = "e90a45fada1b4c7e8d7f16fa0594c80f-UNLOAD";
        assertEquals("e90a45fada1b4c7e8d7f16fa0594c80f", AgvOrchestrationSubtaskIds.stripToPlanTaskId(unload));
    }
}
