package com.librax.lab.module.flow.enums;


public enum RunMode {
    NORMAL,      // 正常流程执行
    STANDALONE,  // 节点单独运行
    COMPENSATE,  // 补偿执行
    MOCK         // Mock 执行，返回预设输出
}
