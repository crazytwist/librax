package com.librax.lab.module.flow.api.callback;

import java.util.Map;

public interface StepCallbackSpi {

    /**
     * 外部回调推进步骤
     * <p>
     * 设备回调、人工审批、外部事件都走这个方法
     *
     * @param executionId   执行实例ID
     * @param nodeId        节点ID
     * @param callbackToken 回调令牌（创建等待时生成，必须匹配）
     * @param success       是否成功
     * @param outputs       输出数据
     * @param errorCode     错误码（失败时）
     * @param errorMsg      错误信息（失败时）
     * @return 推进结果
     */
    CallbackResult callback(String executionId, String nodeId, String callbackToken, boolean success, Map<String, Object> outputs, String errorCode, String errorMsg);


}
