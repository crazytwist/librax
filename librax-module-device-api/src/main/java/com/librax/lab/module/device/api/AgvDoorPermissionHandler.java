package com.librax.lab.module.device.api;

/**
 * 现场机台"可以开门"确认信号扩展点。
 *
 * <p>AGV 到达卸货等待点后会走 {@link AgvWaitSignalHandler} 挂起，
 * 若该编排任务要求开关门，还需要额外等到机台通过这个信号确认"可以开门"，
 * 两个条件都满足才会真正开门并放行 AGV 继续执行卸货（step3）。
 *
 * <p>由业务侧（如 librax-dp）新增的站点信号接收 Controller 调用，
 * taskId 与该编排任务对外暴露的任务号（即流程 executionId）一致。
 */
public interface AgvDoorPermissionHandler {

    /**
     * @param taskId 编排任务的外部任务号（executionId）
     * @return true 表示信号已被业务协调器消费（说明存在对应的编排任务）；
     *         false 表示找不到对应任务，调用方应视为错误处理
     */
    boolean notifyDoorPermission(String taskId);
}
