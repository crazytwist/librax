package com.librax.lab.module.flow.engine.standalone.service;


import com.librax.lab.module.flow.engine.standalone.vo.StandaloneRunReqVO;
import com.librax.lab.module.flow.engine.standalone.vo.StandaloneRunResultVO;

/**
 * 单独运行服务
 *
 * <p>支持从流程中挑出单个节点独立执行，用于开发调试、步骤验证、补偿执行。
 */
public interface StandaloneExecutionService {

    /**
     * 单独运行指定节点
     *
     * @param req 运行请求（流程标识 + 节点ID + mock上下文）
     * @return 运行结果（同步节点直接返回结果，异步节点返回executionId）
     */
    StandaloneRunResultVO run(StandaloneRunReqVO req);
}