package com.librax.lab.module.flow.engine.execution.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志通知通道（兜底实现）
 *
 * <p>开发/测试阶段使用，把通知内容打到日志里。
 * 生产环境配置了真实通道（DINGTALK/WECOM/EMAIL）后，
 * 此通道仍可保留作为 channel=LOG 的显式选择。
 */
@Slf4j
@Component
public class LogNotifyChannel implements NotifyChannel {

    @Override
    public String supportChannel() {
        return "LOG";
    }

    @Override
    public boolean send(NotifyMessage message) {
        log.info("[LogNotify] ===== 通知消息 =====");
        log.info("[LogNotify] 标题: {}", message.getTitle());
        log.info("[LogNotify] 内容: {}", message.getContent());
        log.info("[LogNotify] 收件人: {}", message.getRecipients());
        log.info("[LogNotify] @所有人: {}", message.isAtAll());
        log.info("[LogNotify] ====================");
        return true;
    }
}