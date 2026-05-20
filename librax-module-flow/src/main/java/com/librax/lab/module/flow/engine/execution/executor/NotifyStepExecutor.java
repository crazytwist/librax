package com.librax.lab.module.flow.engine.execution.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.framework.common.util.expression.ExpressionUtil;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    public StepResult execute(StepDispatchContext ctx) {
        Map<String, Object> inputParams = ctx.getInputParams();

        String channelType = getStringParam(inputParams, "channel", "LOG");
        String title       = getStringParam(inputParams, "title", "流程通知");
        String template    = getStringParam(inputParams, "template", "");
        String webhook     = getStringParam(inputParams, "webhook", null);

        // 内置变量注入
        Map<String, Object> templateVars = new HashMap<>(inputParams);
        templateVars.put("nodeId",      ctx.getNodeId());
        templateVars.put("executionId", ctx.getExecutionId());
        templateVars.put("timestamp",   LocalDateTime.now().toString());
        String content = renderTemplate(template, templateVars);

        log.info("[NotifyExecutor] 发送通知 executionId={} nodeId={} channel={} title={}",
                ctx.getExecutionId(), ctx.getNodeId(), channelType, title);

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("channel", channelType);
        outputs.put("title",   title);
        outputs.put("sentAt",  LocalDateTime.now().toString());

        try {
            NotifyChannel channel = findChannel(channelType);
            if (channel == null) {
                logFallback(title, content);
                outputs.put("notifyResult", "FALLBACK_TO_LOG");
                outputs.put("sent", true);
            } else {
                boolean sent = channel.send(NotifyMessage.builder()
                        .channel(channelType).title(title).content(content)
                        .webhook(webhook)
                        .recipients(getListParam(inputParams, "recipients"))
                        .atAll(getBoolParam(inputParams, "atAll", false))
                        .build());
                outputs.put("notifyResult", sent ? "SUCCESS" : "SEND_FAILED");
                outputs.put("sent", sent);
                if (!sent) log.warn("[NotifyExecutor] 通知发送失败 executionId={} nodeId={}",
                        ctx.getExecutionId(), ctx.getNodeId());
            }
        } catch (Exception e) {
            log.error("[NotifyExecutor] 通知异常 executionId={} nodeId={} error={}",
                    ctx.getExecutionId(), ctx.getNodeId(), e.getMessage(), e);
            outputs.put("notifyResult", "ERROR");
            outputs.put("sent",     false);
            outputs.put("errorMsg", e.getMessage());
        }

        return StepResult.ok(outputs);
    }

    private String renderTemplate(String template, Map<String, Object> vars) {
        if (template == null || template.isEmpty()) return "";
        return ExpressionUtil.render(template, vars);
    }

    private NotifyChannel findChannel(String type) {
        return channels.stream()
                .filter(c -> c.supportChannel().equalsIgnoreCase(type))
                .findFirst().orElse(null);
    }

    private void logFallback(String title, String content) {
        log.info("[NotifyExecutor][LOG] title={} content={}", title, content);
    }

    private String getStringParam(Map<String, Object> p, String k, String d) {
        Object v = p.get(k); return v != null ? v.toString() : d;
    }
    private boolean getBoolParam(Map<String, Object> p, String k, boolean d) {
        Object v = p.get(k);
        if (v instanceof Boolean b) return b;
        return v != null ? Boolean.parseBoolean(v.toString()) : d;
    }
    @SuppressWarnings("unchecked")
    private List<String> getListParam(Map<String, Object> p, String k) {
        Object v = p.get(k); return v instanceof List ? (List<String>) v : List.of();
    }
}