package com.librax.lab.module.ai.service.chat.bo;

import com.librax.lab.module.ai.enums.chat.AiChatIntentEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天意图识别结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatIntentRecognizeResult {

    /**
     * 识别出的意图
     */
    private AiChatIntentEnum intent;

    /**
     * 识别理由（简短说明）
     */
    private String reason;

    /**
     * 置信度，范围 0~1
     */
    private Double confidence;

    public static AiChatIntentRecognizeResult of(AiChatIntentEnum intent, String reason, Double confidence) {
        return new AiChatIntentRecognizeResult(intent, reason, confidence);
    }

}
