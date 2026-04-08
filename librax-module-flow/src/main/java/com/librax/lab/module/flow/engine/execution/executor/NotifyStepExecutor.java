package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.engine.execution.model.StepResult;
import com.librax.lab.module.flow.enums.StepTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知步骤执行器
 *
 * <p>根据配置发送通知（钉钉/企微/邮件），通知失败不阻塞流程，始终返回 SUCCESS。
 *
 * <p>参数配置（在 pd_step_definition.default_params 或 pd_pipeline_step.params_override 中）：
 * <pre>
 * {
 *   "channel": "DINGTALK",              // 通道：DINGTALK / WECOM / EMAIL / LOG
 *   "webhook": "https://oapi...",       // DINGTALK/WECOM 的 webhook 地址
 *   "recipients": ["user1", "user2"],   // EMAIL 模式的收件人
 *   "title": "流程通知",                 // 通知标题
 *   "template": "节点 ${nodeId} 已完成，结果: ${result}",  // 消息模板
 *   "atAll": false                      // 是否@所有人（DINGTALK/WECOM）
 * }
 * </pre>
 *
 * <p>模板变量：支持 ${xxx} 占位符，从 inputParams 中替换。
 * 内置变量：${nodeId}、${executionId}、${timestamp}
 *
 * <p>设计原则：
 * <ul>
 *   <li>通知失败只记日志，不返回 FAILED（通知不应该阻塞业务流程）
 *   <li>输出 notifyResult 字段标记实际发送状态，供后续节点判断
 *   <li>通过 NotifyChannel 接口扩展新通道，不需要改执行器
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyStepExecutor implements StepExecutor {

    private final List<NotifyChannel> channels;

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.NOTIFY;
    }

    @Override
    public StepResult execute(StepNode node,
                              String executionId,
                              Map<String, Object> inputParams) {
        // 1. 从 params 取通知配置
        String channelType = getStringParam(inputParams, "channel", "LOG");
        String title = getStringParam(inputParams, "title", "流程通知");
        String template = getStringParam(inputParams, "template", "");
        String webhook = getStringParam(inputParams, "webhook", null);

        // 2. 渲染模板
        Map<String, Object> templateVars = new HashMap<>(inputParams);
        templateVars.put("nodeId", node.getNodeId());
        templateVars.put("executionId", executionId);
        templateVars.put("timestamp", LocalDateTime.now().toString());
        String content = renderTemplate(template, templateVars);

        log.info("[NotifyExecutor] 发送通知 executionId={} nodeId={} channel={} title={}",
                executionId, node.getNodeId(), channelType, title);

        // 3. 查找通道并发送
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("channel", channelType);
        outputs.put("title", title);
        outputs.put("sentAt", LocalDateTime.now().toString());

        try {
            NotifyChannel channel = findChannel(channelType);
            if (channel == null) {
                log.warn("[NotifyExecutor] 未找到通道实现 channel={}, 降级为日志输出",
                        channelType);
                logFallback(title, content);
                outputs.put("notifyResult", "FALLBACK_TO_LOG");
                outputs.put("sent", true);
            } else {
                boolean sent = channel.send(NotifyMessage.builder()
                        .channel(channelType)
                        .title(title)
                        .content(content)
                        .webhook(webhook)
                        .recipients(getListParam(inputParams, "recipients"))
                        .atAll(getBoolParam(inputParams, "atAll", false))
                        .build());

                outputs.put("notifyResult", sent ? "SUCCESS" : "SEND_FAILED");
                outputs.put("sent", sent);

                if (!sent) {
                    log.warn("[NotifyExecutor] 通知发送失败（不阻塞流程） " +
                            "executionId={} nodeId={}", executionId, node.getNodeId());
                }
            }
        } catch (Exception e) {
            log.error("[NotifyExecutor] 通知发送异常（不阻塞流程） " +
                            "executionId={} nodeId={} error={}",
                    executionId, node.getNodeId(), e.getMessage(), e);
            outputs.put("notifyResult", "ERROR");
            outputs.put("sent", false);
            outputs.put("errorMsg", e.getMessage());
        }

        // 4. 始终返回 SUCCESS — 通知不阻塞流程
        return StepResult.ok(outputs);
    }

    // ================================================================
    // 模板渲染
    // ================================================================

    /**
     * 简单模板渲染：${key} → value
     */
    private String renderTemplate(String template, Map<String, Object> vars) {
        if (!StringUtils.hasText(template)) return "";
        String result = template;
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    // ================================================================
    // 通道查找
    // ================================================================

    private NotifyChannel findChannel(String channelType) {
        return channels.stream()
                .filter(c -> c.supportChannel().equalsIgnoreCase(channelType))
                .findFirst()
                .orElse(null);
    }

    private void logFallback(String title, String content) {
        log.info("[NotifyExecutor][LOG通道] title={} content={}", title, content);
    }

    // ================================================================
    // 参数取值工具
    // ================================================================

    private String getStringParam(Map<String, Object> params, String key, String defaultVal) {
        Object val = params.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private boolean getBoolParam(Map<String, Object> params, String key, boolean defaultVal) {
        Object val = params.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return defaultVal;
    }

    @SuppressWarnings("unchecked")
    private List<String> getListParam(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val instanceof List) return (List<String>) val;
        return List.of();
    }
}