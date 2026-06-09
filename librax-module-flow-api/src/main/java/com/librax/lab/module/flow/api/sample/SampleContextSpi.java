package com.librax.lab.module.flow.api.sample;

import java.util.Map;

/**
 * 样本上下文 SPI
 *
 * <p>flow 引擎通过此接口从 lab 模块读取样本相关信息，实现模块间解耦。
 *
 * <p>lab 模块实现此接口，flow 模块通过 Spring 自动注入（允许为空列表）。
 *
 * <p>设计约束：
 * <ul>
 *   <li>实现类必须无副作用（只读），onExecutionBound 的状态写入由 PipelineStartHook 负责
 *   <li>sampleId 不存在时返回空 Map，不抛异常
 *   <li>实现类应做好异常吞噬，不影响主流程调度
 * </ul>
 */
public interface SampleContextSpi {

    /**
     * 根据 sampleId 获取样本实验参数（experiment_params JSON 解析后的 Map）
     *
     * <p>返回的参数会作为步骤 inputParams 的<b>基础层</b>注入，
     * pipeline YAML 中显式配置的 params 优先级更高（会覆盖此返回值）。
     *
     * <p>使用场景：YAML 步骤通过 {@code ${input.targetPh}} 引用，
     * 流程启动时将 sampleId 放入 inputParams 即可自动注入。
     *
     * @param sampleId 样本业务ID，对应 lab_sample_info.sample_id
     * @return 实验参数 Map，无数据时返回空 Map（不返回 null）
     */
    Map<String, Object> getExperimentParams(String sampleId);
}
