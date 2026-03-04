package com.librax.lab.module.flow.enums;

import com.librax.lab.framework.common.exception.ErrorCode;

/**
 * Resource 错误码枚举类
 * <p>
 * flow 系统，使用 1-007-000-000 段
 */
public interface ErrorCodeConstants {

    ErrorCode FLOW_DEFINITION_NOT_EXISTS = new ErrorCode(1_007_000_001, "流程定义不存在");
    ErrorCode NODE_DEFINITION_NOT_EXISTS = new ErrorCode(1_007_000_002, "节点定义不存在");
}
