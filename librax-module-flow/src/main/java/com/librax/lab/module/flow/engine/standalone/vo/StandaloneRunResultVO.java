package com.librax.lab.module.flow.engine.standalone.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 单独运行结果
 */
@Data
@Builder
public class StandaloneRunResultVO {

    /** 执行实例ID */
    private String executionId;

    /** 是否异步（INSTRUMENT/WAIT 等返回 WAITING 的节点） */
    private boolean async;

    /**
     * 同步节点的执行结果（async=false 时有值）
     * <p>async=true 时为 null，需要通过 executionId 查询或等回调
     */
    private Boolean success;

    /** 同步节点的输出数据 */
    private Map<String, Object> outputs;

    /** 错误码（失败时） */
    private String errorCode;

    /** 错误信息（失败时） */
    private String errorMsg;

    /** 节点当前状态 */
    private String stepStatus;

    // ---- 快捷构建 ----

    public static StandaloneRunResultVO async(String executionId) {
        return StandaloneRunResultVO.builder()
                .executionId(executionId)
                .async(true)
                .stepStatus("WAITING")
                .build();
    }

    public static StandaloneRunResultVO syncSuccess(String executionId,
                                                    Map<String, Object> outputs) {
        return StandaloneRunResultVO.builder()
                .executionId(executionId)
                .async(false)
                .success(true)
                .outputs(outputs)
                .stepStatus("SUCCESS")
                .build();
    }

    public static StandaloneRunResultVO syncFailed(String executionId,
                                                   String errorCode,
                                                   String errorMsg) {
        return StandaloneRunResultVO.builder()
                .executionId(executionId)
                .async(false)
                .success(false)
                .errorCode(errorCode)
                .errorMsg(errorMsg)
                .stepStatus("FAILED")
                .build();
    }
}