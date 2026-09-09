package com.librax.lab.module.ai.service.chat;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.librax.lab.framework.common.util.json.JsonUtils;
import com.librax.lab.module.ai.dal.dataobject.model.AiModelDO;
import com.librax.lab.module.ai.enums.chat.AiChatIntentEnum;
import com.librax.lab.module.ai.enums.model.AiPlatformEnum;
import com.librax.lab.module.ai.service.chat.bo.AiChatIntentRecognizeResult;
import com.librax.lab.module.ai.util.AiUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 聊天意图识别 Service 实现
 */
@Service
@Slf4j
public class AiChatIntentRecognizeServiceImpl implements AiChatIntentRecognizeService {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{.*}", Pattern.DOTALL);

    /**
     * 意图识别使用较低温度，提升分类稳定性
     */
    private static final double INTENT_TEMPERATURE = 0.1;

    private static final int INTENT_MAX_TOKENS = 256;

    private static final String INTENT_SYSTEM_PROMPT = """
            你是实验室管理系统（LibraX）的意图识别助手。
            请判断用户问题属于哪个意图类别，并且只输出 JSON，不要输出任何其它文字、Markdown 或代码块标记。

            JSON 格式：
            {"intent":"DEVICE|RESOURCE|FLOW|OTHER","reason":"不超过30字的判断理由","confidence":0.0}

            类别说明：
            - DEVICE：设备/仪器相关，例如设备信息、指令下发、编解码、驱动、联机状态、设备故障等
            - RESOURCE：资源/物料相关，例如物料、耗材、库位、库存、取用配置等
            - FLOW：流程/流水线相关，例如 Pipeline、步骤编排、执行调度、样品检测流程、回调与重试等
            - OTHER：无法归入以上三类的通用问题

            confidence 取值范围 0~1，表示你对该分类的把握程度。
            """;

    @Override
    public AiChatIntentRecognizeResult recognize(ChatModel chatModel, AiModelDO model, String content) {
        if (chatModel == null || model == null || StrUtil.isBlank(content)) {
            return AiChatIntentRecognizeResult.of(AiChatIntentEnum.OTHER, "问题为空，默认其他", 0D);
        }
        try {
            AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
            ChatOptions options = AiUtils.buildChatOptions(platform, model.getModel(),
                    INTENT_TEMPERATURE, INTENT_MAX_TOKENS);
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(INTENT_SYSTEM_PROMPT),
                    new UserMessage(content)
            ), options);
            ChatResponse response = chatModel.call(prompt);
            String raw = AiUtils.getChatResponseContent(response);
            IntentJson intentJson = parseIntentJson(raw);
            AiChatIntentEnum intent = AiChatIntentEnum.ofCodeOrDefault(
                    intentJson != null ? intentJson.getIntent() : null);
            String reason = intentJson != null && StrUtil.isNotBlank(intentJson.getReason())
                    ? intentJson.getReason() : "未给出识别理由";
            Double confidence = intentJson != null ? intentJson.getConfidence() : null;
            return AiChatIntentRecognizeResult.of(intent, reason, confidence);
        } catch (Exception ex) {
            log.warn("[recognize][意图识别失败，content={}]", content, ex);
            return AiChatIntentRecognizeResult.of(AiChatIntentEnum.OTHER, "意图识别失败，回退其他", 0D);
        }
    }

    private IntentJson parseIntentJson(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String text = StrUtil.trim(raw);
        // 兼容模型偶发包一层 ```json ... ```
        text = StrUtil.removePrefix(text, "```json");
        text = StrUtil.removePrefix(text, "```");
        text = StrUtil.removeSuffix(text, "```");
        text = StrUtil.trim(text);
        IntentJson result = JsonUtils.parseObjectQuietly(text, new TypeReference<IntentJson>() {});
        if (result != null) {
            return result;
        }
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return JsonUtils.parseObjectQuietly(matcher.group(), new TypeReference<IntentJson>() {});
        }
        return null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class IntentJson {

        private String intent;
        private String reason;
        private Double confidence;

    }

}
