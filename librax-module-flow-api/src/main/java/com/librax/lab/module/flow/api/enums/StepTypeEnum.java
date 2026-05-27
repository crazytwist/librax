package com.librax.lab.module.flow.api.enums;


public enum StepTypeEnum {
    INSTRUMENT,     // 仪器执行，MQ 异步
    COMPUTE,        // 本地计算，Bean 或 LiteFlow
    CONDITION,      // 条件分支，Aviator 表达式求值
    WAIT,           // 等待，固定时长或外部信号
    NOTIFY,         // 通知，异步发送
    SAMPLE_SPLIT,   // 样本拆分
    MANUAL,         // 人工处理
    UNIT_LAUNCHER   // 执行单元启动器，启动子流程
}
