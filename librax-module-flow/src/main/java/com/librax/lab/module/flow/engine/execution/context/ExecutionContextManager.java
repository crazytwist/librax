
package com.librax.lab.module.flow.engine.execution.context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.librax.lab.module.flow.dal.dataobject.executioncontext.ExecutionContextDO;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.module.flow.dal.mysql.executioncontext.ExecutionContextMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionContextManager {

    private final StringRedisTemplate redisTemplate;
    private final ExecutionContextMapper contextMapper;

    // Redis key：lab:ctx:{executionId}
    // 结构：Hash，field = nodeId，value = JSON字符串
    private static final String KEY_PREFIX = "lab:ctx:";
    // 上下文在 Redis 中的过期时间（流程一般不超过24小时）
    private static final long EXPIRE_HOURS = 48;

    // ----------------------------------------------------------------
    // 写入：步骤 SUCCESS 后将输出写入上下文
    // ----------------------------------------------------------------

    /**
     * 写入节点输出到上下文
     * 同步写 Redis，异步持久化到 DB
     *
     * @param executionId 执行实例ID
     * @param nodeId      节点ID（作为 Redis Hash 的 field）
     * @param outputs     节点输出数据
     */
    public void putNodeOutput(String executionId,
                              String nodeId,
                              Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) return;

        String redisKey = redisKey(executionId);
        String outputJson = JSON.toJSONString(outputs);

        // 1. 同步写 Redis Hash（field = nodeId，value = JSON）
        redisTemplate.opsForHash().put(redisKey, nodeId, outputJson);
        // 重置过期时间
        redisTemplate.expire(redisKey, EXPIRE_HOURS, TimeUnit.HOURS);

        log.debug("[ContextManager] 写入上下文 executionId={} nodeId={}", executionId, nodeId);

        // 2. 异步持久化到 DB（不阻塞调度主链路）
        asyncPersistToDB(executionId, nodeId, outputJson);
    }

    // ----------------------------------------------------------------
    // 读取：步骤执行前从上下文取前置节点输出
    // ----------------------------------------------------------------

    /**
     * 获取某个节点的全部输出
     *
     * @param executionId 执行实例ID
     * @param nodeId      前置节点ID
     * @return 节点输出 Map，不存在返回空 Map
     */
    public Map<String, Object> getNodeOutput(String executionId, String nodeId) {
        String redisKey = redisKey(executionId);

        // 1. 优先从 Redis 取
        Object val = redisTemplate.opsForHash().get(redisKey, nodeId);
        if (val != null) {
            return JSON.parseObject(val.toString(),
                    new TypeReference<Map<String, Object>>() {
                    });
        }

        // 2. Redis miss，从 DB 恢复（断点恢复场景）
        log.info("[ContextManager] Redis miss，从 DB 恢复 executionId={} nodeId={}",
                executionId, nodeId);
        return getNodeOutputFromDB(executionId, nodeId);
    }

    /**
     * 获取整个流程的上下文（所有节点输出汇总）
     * 断点恢复时用来重建完整上下文
     *
     * @param executionId 执行实例ID
     * @return Map<nodeId, Map<field, value>>
     */
    public Map<String, Map<String, Object>> getAllOutputs(String executionId) {
        String redisKey = redisKey(executionId);

        // 1. 从 Redis Hash 取全部 field
        Map<Object, Object> redisData = redisTemplate.opsForHash().entries(redisKey);
        if (!redisData.isEmpty()) {
            Map<String, Map<String, Object>> result = new HashMap<>();
            redisData.forEach((k, v) -> result.put(
                    k.toString(),
                    JSON.parseObject(v.toString(),
                            new TypeReference<Map<String, Object>>() {
                            })));
            return result;
        }

        // 2. Redis 全 miss，从 DB 重建
        log.info("[ContextManager] Redis 全 miss，从 DB 重建上下文 executionId={}", executionId);
        return getAllOutputsFromDB(executionId);
    }

    /**
     * 解析 input_mapping 表达式，从上下文取值
     * 支持 ${nodeId.fieldName} 语法
     *
     * @param executionId  执行实例ID
     * @param inputMapping 如 {"phValue": "${s_ph.ph}"}
     * @param inputParams  流程初始参数（处理 ${input.xxx} 引用）
     * @return 解析后的实际入参 Map
     */
    public Map<String, Object> resolveInputMapping(String executionId,
                                                   Map<String, String> inputMapping,
                                                   Map<String, Object> inputParams) {
        Map<String, Object> resolved = new HashMap<>();
        if (inputMapping == null || inputMapping.isEmpty()) return resolved;

        inputMapping.forEach((paramName, expr) -> {
            Object value = resolveExpression(executionId, expr, inputParams);
            resolved.put(paramName, value);
        });
        return resolved;
    }

    // ----------------------------------------------------------------
    // 清理：流程结束后清理 Redis（DB 保留）
    // ----------------------------------------------------------------

    public void cleanup(String executionId) {
        redisTemplate.delete(redisKey(executionId));
        log.info("[ContextManager] Redis 上下文已清理 executionId={}", executionId);
    }

    // ----------------------------------------------------------------
    // 私有方法
    // ----------------------------------------------------------------

    /**
     * 解析单个表达式
     * ${s_ph.ph}    → 取 executionId 上下文中 s_ph 节点的 ph 字段
     * ${input.xxx}  → 取流程初始参数中的 xxx 字段
     * 其他          → 当作字面量直接返回
     */
    private Object resolveExpression(String executionId,
                                     String expr,
                                     Map<String, Object> inputParams) {
        if (!StringUtils.hasText(expr)) return expr;

        // 匹配 ${xxx.yyy} 格式
        if (expr.startsWith("${") && expr.endsWith("}")) {
            String inner = expr.substring(2, expr.length() - 1); // 去掉 ${ 和 }
            int dotIdx = inner.indexOf('.');
            if (dotIdx < 0) return expr; // 格式不对，原样返回

            String prefix = inner.substring(0, dotIdx);   // nodeId 或 "input"
            String fieldName = inner.substring(dotIdx + 1);  // 字段名

            if ("input".equals(prefix)) {
                // 取流程初始参数
                return inputParams != null ? inputParams.get(fieldName) : null;
            } else {
                // 取前置节点输出
                Map<String, Object> nodeOutput = getNodeOutput(executionId, prefix);
                return nodeOutput.get(fieldName);
            }
        }

        return expr; // 非表达式，字面量直接返回
    }

    /**
     * 异步持久化到 DB（追加写，不覆盖其他节点数据）
     */
    @Async
    protected void asyncPersistToDB(String executionId,
                                    String nodeId,
                                    String outputJson) {
        try {
            // 用 MySQL JSON_SET 追加写入，不覆盖其他 nodeId 的数据
            // 如果记录不存在先插入，存在则 JSON_SET
            ExecutionContextDO existing = contextMapper.selectByExecutionId(executionId);
            if (existing == null) {
                LocalDateTime now = LocalDateTime.now();
                ExecutionContextDO record = new ExecutionContextDO();
                record.setExecutionId(executionId);
                record.setContextData(JSON.toJSONString(Map.of(nodeId, JSON.parseObject(outputJson))));
                record.setCreator("SYSTEM");
                record.setUpdater("SYSTEM");
                record.setCreateTime(now);
                record.setUpdateTime(now);
                record.setDeleted(false);
                contextMapper.insert(record);
            } else {
                // 追加：JSON_SET 写入当前节点数据
                contextMapper.appendNodeOutput(executionId, nodeId, outputJson);
            }
        } catch (Exception e) {
            log.error("[ContextManager] DB 持久化失败 executionId={} nodeId={} error={}",
                    executionId, nodeId, e.getMessage());
        }
    }

    private Map<String, Object> getNodeOutputFromDB(String executionId, String nodeId) {
        ExecutionContextDO ctx = contextMapper.selectByExecutionId(executionId);
        if (ctx == null || !StringUtils.hasText(ctx.getContextData())) return Map.of();

        Map<String, Map<String, Object>> all = JSON.parseObject(ctx.getContextData(),
                new TypeReference<Map<String, Map<String, Object>>>() {
                });
        return all.getOrDefault(nodeId, Map.of());
    }

    private Map<String, Map<String, Object>> getAllOutputsFromDB(String executionId) {
        ExecutionContextDO ctx = contextMapper.selectByExecutionId(executionId);
        if (ctx == null || !StringUtils.hasText(ctx.getContextData())) return Map.of();

        return JSON.parseObject(ctx.getContextData(),
                new TypeReference<Map<String, Map<String, Object>>>() {
                });
    }

    private String redisKey(String executionId) {
        return KEY_PREFIX + executionId;
    }
}