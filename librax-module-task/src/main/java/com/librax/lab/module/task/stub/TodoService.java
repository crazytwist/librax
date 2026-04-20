package com.librax.lab.module.task.stub;


/**
 * 待办服务占位接口
 * TODO: 后续对接审批系统（Flowable BPM / 自研工单系统）
 */
public interface TodoService {

    /**
     * 创建人工待办
     *
     * @param req 待办创建请求
     * @return todoId 待办唯一ID，写入 lab_task.external_task_id
     */
    String create(TodoCreateReq req);

    /**
     * 取消待办
     *
     * @param todoId 待办ID
     */
    void cancel(String todoId);
}
