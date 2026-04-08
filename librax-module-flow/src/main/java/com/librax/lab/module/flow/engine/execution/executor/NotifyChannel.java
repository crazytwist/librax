package com.librax.lab.module.flow.engine.execution.executor;


/**
 * 通知通道接口
 *
 * <p>每种通知方式实现此接口并加 @Component 即可自动注册。
 * NotifyStepExecutor 通过 supportChannel() 匹配通道。
 *
 * <p>内置实现：
 * <ul>
 *   <li>LogNotifyChannel  — 日志输出（开发兜底）
 *   <li>DingTalkNotifyChannel — 钉钉机器人 webhook
 *   <li>WeComNotifyChannel — 企业微信机器人 webhook
 *   <li>EmailNotifyChannel — 邮件发送
 * </ul>
 */
public interface NotifyChannel {

    /**
     * 通道标识，对应参数中的 channel 字段
     * 如 "DINGTALK"、"WECOM"、"EMAIL"、"LOG"
     */
    String supportChannel();

    /**
     * 发送通知
     *
     * @param message 通知消息
     * @return true=发送成功 false=发送失败
     */
    boolean send(NotifyMessage message);
}