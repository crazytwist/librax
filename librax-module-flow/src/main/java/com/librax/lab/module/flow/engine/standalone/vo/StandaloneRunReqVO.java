package com.librax.lab.module.flow.engine.standalone.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 单独运行请求
 */
@Data
public class StandaloneRunReqVO {

    /** 流程标识 */
    @NotBlank(message = "pipelineKey 不能为空")
    private String pipelineKey;

    /** 流程版本 */
    @NotNull(message = "pipelineVersion 不能为空")
    private Integer pipelineVersion;

    /** 要单独运行的节点ID */
    @NotBlank(message = "nodeId 不能为空")
    private String nodeId;

    /**
     * Mock 上下文：模拟前置节点的输出
     *
     * <p>key = 前置节点的 nodeId，value = 该节点的输出 Map
     * <p>示例：{"s_ph": {"ph": 7.2, "temperature": 25.0}, "input": {"sampleId": "S001"}}
     *
     * <p>如果不传，会尝试使用 pd_step_definition.mock_output 里的默认值
     */
    private Map<String, Map<String, Object>> mockContext;

    /**
     * 流程初始参数（模拟 input_params）
     * <p>会被写入上下文的 "input" key 下，供 ${input.xxx} 引用
     */
    private Map<String, Object> inputParams;

    /** 触发人 */
    private String triggeredBy;

    /**
     * 父执行ID（可选）
     * <p>从某个已有流程的上下文中补跑某个节点时填写，
     * 会自动从父执行的上下文中拉取数据作为本次运行的上下文
     */
    private String parentExecutionId;
}