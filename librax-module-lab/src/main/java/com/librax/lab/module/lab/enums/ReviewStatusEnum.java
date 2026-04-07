package com.librax.lab.module.lab.enums;

public enum ReviewStatusEnum {

    PENDING,        // 待审核
    AUTO_APPROVED,  // 自动通过（在参考范围内）
    APPROVED,       // 人工通过
    REJECTED,       // 驳回
    RETEST;         // 需复测

}
