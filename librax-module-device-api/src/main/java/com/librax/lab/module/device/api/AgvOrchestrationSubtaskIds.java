package com.librax.lab.module.device.api;

/**
 * 仓储编排（补料/下料）下发给 AGV 的子任务 taskId 格式解析。
 *
 * <p>与流程引擎 executionId（通常无后缀）区分；waitSignal 兜底不得把带子后缀的 taskId 当作 executionId。
 */
public final class AgvOrchestrationSubtaskIds {

    private AgvOrchestrationSubtaskIds() {
    }

    /** 是否为仓储编排派生的 AGV 子任务 taskId（非流程 executionId）。 */
    public static boolean isOrchestrationSubtaskAgvTaskId(String agvTaskId) {
        return stripToPlanTaskId(agvTaskId) != null;
    }

    /**
     * 从 AGV 子任务 taskId 还原 planTaskId；非已知编排子任务格式时返回 null。
     */
    public static String stripToPlanTaskId(String agvTaskId) {
        if (agvTaskId == null || agvTaskId.isBlank()) {
            return null;
        }
        int cut = indexOfSubtaskSuffix(agvTaskId);
        if (cut <= 0) {
            return null;
        }
        return agvTaskId.substring(0, cut);
    }

    private static int indexOfSubtaskSuffix(String agvTaskId) {
        int unloadDetail = agvTaskId.indexOf("-UNLOAD-");
        if (unloadDetail > 0) {
            return unloadDetail;
        }
        if (agvTaskId.endsWith("-UNLOAD")) {
            return agvTaskId.length() - "-UNLOAD".length();
        }
        int move = agvTaskId.indexOf("-MOVE");
        if (move > 0) {
            return move;
        }
        int preposition = agvTaskId.indexOf("-PREPOSITION");
        if (preposition > 0) {
            return preposition;
        }
        int loadWave = agvTaskId.indexOf("-LOAD-");
        if (loadWave > 0) {
            return loadWave;
        }
        if (agvTaskId.endsWith("-LOAD")) {
            return agvTaskId.length() - "-LOAD".length();
        }
        return -1;
    }
}
