package com.librax.lab.module.infra.mdc;

import org.slf4j.MDC;

public final class ExecutionMdc {

    public static final String EXECUTION_ID = "executionId";
    public static final String NODE_ID      = "nodeId";
    public static final String ATTEMPT      = "attempt";

    private ExecutionMdc() {}

    public static void set(String executionId, String nodeId, int attempt) {
        MDC.put(EXECUTION_ID, executionId);
        MDC.put(NODE_ID,      nodeId != null ? nodeId : "");
        MDC.put(ATTEMPT,      String.valueOf(attempt));
    }

    public static void set(String executionId) {
        MDC.put(EXECUTION_ID, executionId);
    }

    public static void clear() {
        MDC.remove(EXECUTION_ID);
        MDC.remove(NODE_ID);
        MDC.remove(ATTEMPT);
    }

    /** 在线程池里用，避免父线程的 MDC 泄漏到子线程 */
    public static Runnable wrap(Runnable task, String executionId,
                                String nodeId, int attempt) {
        return () -> {
            set(executionId, nodeId, attempt);
            try {
                task.run();
            } finally {
                clear();
            }
        };
    }
}