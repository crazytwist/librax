package com.librax.lab.module.flow.enums;


public enum FailStrategyEnum {
    FAIL_FAST,       // 任意节点失败立即终止整条流程
    SKIP, CONTINUE_ON_FAIL // 节点失败后跳过，继续执行无依赖的其他节点
}

