package com.librax.lab.module.device.api;

/** AGV现场动作完成后恢复任务的业务扩展点。 */
public interface AgvResumeHandler {
    boolean resume(String taskId);
}
