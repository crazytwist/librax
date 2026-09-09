package com.librax.lab.module.ai.enums.chat;

import cn.hutool.core.util.ArrayUtil;
import com.librax.lab.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI 聊天意图枚举
 * <p>
 * 对话开始时识别用户问题所属业务域，用于后续回答引导。
 */
@Getter
@AllArgsConstructor
public enum AiChatIntentEnum implements ArrayValuable<String> {

    DEVICE("DEVICE", "设备", """
            用户问题与实验室设备/仪器相关。请围绕设备信息、设备指令、编解码、驱动、联机状态、下发与回调等方面回答；
            如信息不足，主动追问设备编号、型号或当前操作场景。
            """),

    RESOURCE("RESOURCE", "资源", """
            用户问题与实验室资源/物料相关。请围绕物料、耗材配置、库位、库存与取用等方面回答；
            如信息不足，主动追问物料名称、编码或库位信息。
            """),

    FLOW("FLOW", "流程", """
            用户问题与实验室检测流程/流水线相关。请围绕 Pipeline 定义、步骤编排、执行调度、状态流转、重试恢复与结果回调等方面回答；
            如信息不足，主动追问流程名称、步骤或样品相关上下文。
            """),

    OTHER("OTHER", "其他", """
            用户问题不属于设备、资源或流程业务域。请以通用助手身份友好回答，必要时引导用户补充与实验室业务相关的具体问题。
            """),
    ;

    public static final String[] ARRAYS = Arrays.stream(values()).map(AiChatIntentEnum::getCode).toArray(String[]::new);

    /**
     * 意图编码（持久化值）
     */
    private final String code;
    /**
     * 意图名称
     */
    private final String name;
    /**
     * 注入到对话 System Prompt 的领域引导
     */
    private final String systemGuidance;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static AiChatIntentEnum valueOfCode(String code) {
        return ArrayUtil.firstMatch(item -> item.getCode().equalsIgnoreCase(code), values());
    }

    public static AiChatIntentEnum ofCodeOrDefault(String code) {
        AiChatIntentEnum intent = valueOfCode(code);
        return intent != null ? intent : OTHER;
    }

}
