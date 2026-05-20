package com.librax.lab.module.flow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 样本模式枚举
 *
 * 在 pd_pipeline_definition.sample_mode 字段使用
 */
@Getter
@AllArgsConstructor
public enum SampleModeEnum {

    NONE("NONE", "无样本模式 — 纯设备操作流程，跳过所有样本逻辑"),
    OPTIONAL("OPTIONAL", "可选样本 — 有样本时追溯，无样本时正常执行"),
    REQUIRED("REQUIRED", "必须样本 — 流程必须关联样本，可以在启动时或指定节点绑定");

    private final String code;
    private final String desc;

    public static SampleModeEnum of(String code) {
        if (code == null) return REQUIRED; // 默认保持原有行为
        for (SampleModeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return REQUIRED;
    }
}