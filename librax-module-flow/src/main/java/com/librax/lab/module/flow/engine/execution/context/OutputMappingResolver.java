package com.librax.lab.module.flow.engine.execution.context;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 输出映射解析器
 *
 * <p>将执行器返回的原始 outputs 按照 output_mapping 配置进行字段提取和重命名。
 *
 * <p>output_mapping 格式：
 * <pre>
 *   key   = 映射后的字段名（写入上下文的 key）
 *   value = JSONPath 表达式（从 outputs 中提取值的路径）
 * </pre>
 *
 * <p>示例：
 * <pre>
 *   output_mapping = {"ph": "$.result.phValue", "temperature": "$.result.tempValue"}
 *   outputs = {"result": {"phValue": 7.35, "tempValue": 25.0}, "status": "OK"}
 *
 *   解析结果 = {"ph": 7.35, "temperature": 25.0}
 * </pre>
 *
 * <p>如果 output_mapping 为 null 或空，直接返回原始 outputs（向后兼容）。
 */
@Slf4j
@Component
public class OutputMappingResolver {

    /**
     * 应用 output_mapping，从原始 outputs 中提取并重命名字段
     *
     * @param rawOutputs    执行器返回的原始输出
     * @param outputMapping 输出映射配置，key=目标字段名 value=JSONPath表达式
     * @return 映射后的输出，用于写入上下文
     */
    public Map<String, Object> resolve(Map<String, Object> rawOutputs,
                                       Map<String, String> outputMapping) {
        // 没有配置 output_mapping，原样返回
        if (outputMapping == null || outputMapping.isEmpty()) {
            return rawOutputs;
        }

        if (rawOutputs == null || rawOutputs.isEmpty()) {
            return new HashMap<>();
        }

        // 将 outputs 转为 JSON 文档，供 JSONPath 查询
        Object document;
        try {
            document = com.jayway.jsonpath.Configuration.defaultConfiguration()
                    .jsonProvider().parse(com.alibaba.fastjson.JSON.toJSONString(rawOutputs));
        } catch (Exception e) {
            log.error("[OutputMapping] outputs 转 JSON 失败，返回原始数据", e);
            return rawOutputs;
        }

        Map<String, Object> mapped = new HashMap<>();

        for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
            String targetKey = entry.getKey();     // 映射后的字段名
            String jsonPath = entry.getValue();     // JSONPath 表达式

            try {
                Object value = JsonPath.read(document, jsonPath);
                mapped.put(targetKey, value);
                log.debug("[OutputMapping] {} = {} → {}", targetKey, jsonPath, value);
            } catch (PathNotFoundException e) {
                // 路径不存在，跳过该字段，记录警告
                log.warn("[OutputMapping] JSONPath 未找到值: key={} path={}", targetKey, jsonPath);
                mapped.put(targetKey, null);
            } catch (Exception e) {
                log.error("[OutputMapping] JSONPath 解析异常: key={} path={}", targetKey, jsonPath, e);
                mapped.put(targetKey, null);
            }
        }

        return mapped;
    }
}