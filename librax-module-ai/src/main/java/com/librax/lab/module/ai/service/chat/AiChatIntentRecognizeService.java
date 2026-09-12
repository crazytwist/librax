package com.librax.lab.module.ai.service.chat;

import com.librax.lab.module.ai.dal.dataobject.model.AiModelDO;
import com.librax.lab.module.ai.service.chat.bo.AiChatIntentRecognizeResult;
import org.springframework.ai.chat.model.ChatModel;

/**
 * AI 聊天意图识别 Service
 */
public interface AiChatIntentRecognizeService {

    /**
     * 识别用户问题意图（设备 / 资源 / 流程 / 其他）
     *
     * @param chatModel 聊天模型
     * @param model     模型配置
     * @param content   用户问题
     * @return 识别结果，失败时回退为 OTHER
     */
    AiChatIntentRecognizeResult recognize(ChatModel chatModel, AiModelDO model, String content);

}
