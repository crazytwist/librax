package com.librax.lab.module.task.executor;

import com.librax.lab.module.task.dal.dataobject.task.TaskDO;

/**
 * 任务执行器接口
 *
 * <p>每种 taskType 对应一个实现，由 TaskExecutorFactory 自动注册。
 * TaskRouter 消费队列时通过此接口执行具体任务。
 *
 * <p>实现约定：
 * <ul>
 *   <li>execute() 是非阻塞的，发出指令/入队后立即返回
 *   <li>任务完成后通过 TaskCallbackDispatcher.dispatch() 通知引擎
 *   <li>cancel() 尽力而为，不保证一定能终止
 * </ul>
 */
public interface TaskExecutor {

    /** 支持的任务类型，对应 lab_task.task_type */
    String supportType();

    /** 执行任务（非阻塞） */
    void execute(TaskDO task);

    /** 取消任务（超时/流程取消时调用） */
    void cancel(TaskDO task);
}