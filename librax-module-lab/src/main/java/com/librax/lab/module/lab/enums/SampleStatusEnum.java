package com.librax.lab.module.lab.enums;

public enum SampleStatusEnum {
    REGISTERED,   // 已登记
    LOADED,       // 已装载（进入流程）
    IN_PROCESS,   // 处理中
    SPLIT,        // 已拆分（由子样本继续流转）
    COMPLETED,    // 已完成
    ARCHIVED,     // 已归档
    REJECTED,     // 不合格
    LOST;         // 丢失
}
