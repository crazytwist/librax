package com.librax.lab.module.flow.engine.execution.executor;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 通知消息体
 */
@Data
@Builder
public class NotifyMessage {

    /** 通道类型 */
    private String channel;

    /** 通知标题 */
    private String title;

    /** 通知内容（已渲染模板） */
    private String content;

    /** webhook 地址（DINGTALK/WECOM） */
    private String webhook;

    /** 收件人列表（EMAIL） */
    private List<String> recipients;

    /** 是否@所有人（DINGTALK/WECOM） */
    private boolean atAll;
}