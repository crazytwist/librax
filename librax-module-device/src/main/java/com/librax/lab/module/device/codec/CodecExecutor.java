package com.librax.lab.module.device.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.JSONPath;
import com.librax.lab.module.device.dal.dataobject.devicecodec.DeviceCodecDO;
import com.librax.lab.module.device.dal.mysql.devicecodec.DeviceCodecMapper;
import com.librax.lab.module.device.exception.DeviceException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设备响应解析器
 *
 * <p>根据 {@link DeviceCodecDO} 中配置的解析规则，将设备原始响应转换为结构化的 Map 输出。
 * 支持 JSON（JSONPath）、HEX（按位提取）、REGEX（正则捕获）、SCRIPT（Groovy/JS 脚本）四种解析方式。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodecExecutor {

    private final DeviceCodecMapper codecMapper;
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    /**
     * 解析设备原始响应
     *
     * @param codecId     解析规则 ID，为 null 时直接透传原始响应
     * @param rawResponse 设备原始响应字符串
     * @return 结构化输出
     */
    public Map<String, Object> parse(Long codecId, String rawResponse) {
        if (codecId == null) {
            return Map.of("raw", rawResponse != null ? rawResponse : "");
        }

        DeviceCodecDO codec = codecMapper.selectById(codecId);
        if (codec == null) {
            throw new DeviceException("CODEC_NOT_FOUND", "解析规则不存在: " + codecId);
        }

        Map<String, Object> result;
        String parseType = codec.getParseType();

        switch (parseType) {
            case "JSON":
                result = parseJson(codec.getFieldMapping(), rawResponse);
                break;
            case "REGEX":
                result = parseRegex(codec.getRegexRules(), rawResponse);
                break;
            case "HEX":
                result = parseHex(codec.getHexRules(), rawResponse);
                break;
            case "SCRIPT":
                result = parseScript(codec.getScriptEngine(), codec.getScriptContent(), rawResponse);
                break;
            default:
                log.warn("[CodecExecutor] 未知的解析类型: {}, 透传原始响应", parseType);
                return Map.of("raw", rawResponse != null ? rawResponse : "");
        }

        // 后处理：单位换算
        if (codec.getUnitConversions() != null) {
            applyUnitConversions(result, codec.getUnitConversions());
        }

        // 后处理：有效范围校验
        if (codec.getValidRange() != null) {
            applyValidRange(result, codec.getValidRange());
        }

        return result;
    }

    // ----------------------------------------------------------------
    // JSON 解析：使用 JSONPath 提取字段
    // ----------------------------------------------------------------

    private Map<String, Object> parseJson(String fieldMappingJson, String rawResponse) {
        if (fieldMappingJson == null || rawResponse == null) {
            return Collections.emptyMap();
        }
        Map<String, String> fieldMapping = JSON.parseObject(fieldMappingJson,
                new TypeReference<Map<String, String>>() {});

        Object document = JSON.parse(rawResponse);

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            try {
                Object value = JSONPath.eval(document, entry.getValue());
                result.put(entry.getKey(), value);
            } catch (Exception e) {
                log.warn("[CodecExecutor] JSONPath 提取失败 field={} path={}: {}",
                        entry.getKey(), entry.getValue(), e.getMessage());
                result.put(entry.getKey(), null);
            }
        }
        return result;
    }

    // ----------------------------------------------------------------
    // REGEX 解析：正则捕获组提取
    // ----------------------------------------------------------------

    private Map<String, Object> parseRegex(String regexRulesJson, String rawResponse) {
        if (regexRulesJson == null || rawResponse == null) {
            return Collections.emptyMap();
        }
        Map<String, String> regexRules = JSON.parseObject(regexRulesJson,
                new TypeReference<Map<String, String>>() {});

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : regexRules.entrySet()) {
            try {
                Matcher matcher = Pattern.compile(entry.getValue()).matcher(rawResponse);
                if (matcher.find() && matcher.groupCount() >= 1) {
                    result.put(entry.getKey(), matcher.group(1));
                } else {
                    result.put(entry.getKey(), null);
                }
            } catch (Exception e) {
                log.warn("[CodecExecutor] 正则提取失败 field={} regex={}: {}",
                        entry.getKey(), entry.getValue(), e.getMessage());
                result.put(entry.getKey(), null);
            }
        }
        return result;
    }

    // ----------------------------------------------------------------
    // HEX 解析：按位提取
    // ----------------------------------------------------------------

    private Map<String, Object> parseHex(String hexRulesJson, String rawResponse) {
        if (hexRulesJson == null || rawResponse == null) {
            return Collections.emptyMap();
        }
        List<HexRule> rules = JSON.parseArray(hexRulesJson, HexRule.class);
        // 去除空格和分隔符，得到纯十六进制字符串
        String hex = rawResponse.replaceAll("[\\s\\-:]", "");
        byte[] bytes = hexToBytes(hex);

        Map<String, Object> result = new LinkedHashMap<>();
        for (HexRule rule : rules) {
            try {
                if (rule.getByteOffset() + rule.getByteLength() > bytes.length) {
                    log.warn("[CodecExecutor] HEX 偏移越界 field={} offset={} len={} total={}",
                            rule.getFieldName(), rule.getByteOffset(), rule.getByteLength(), bytes.length);
                    result.put(rule.getFieldName(), null);
                    continue;
                }
                byte[] slice = Arrays.copyOfRange(bytes,
                        rule.getByteOffset(), rule.getByteOffset() + rule.getByteLength());
                result.put(rule.getFieldName(), convertBytes(slice, rule.getDataType()));
            } catch (Exception e) {
                log.warn("[CodecExecutor] HEX 解析失败 field={}: {}", rule.getFieldName(), e.getMessage());
                result.put(rule.getFieldName(), null);
            }
        }
        return result;
    }

    private Object convertBytes(byte[] slice, String dataType) {
        ByteBuffer buf = ByteBuffer.wrap(slice).order(ByteOrder.BIG_ENDIAN);
        if (dataType == null) dataType = "INT16";
        switch (dataType.toUpperCase()) {
            case "INT8":
                return (int) slice[0];
            case "INT16":
                return buf.getShort();
            case "INT32":
                return buf.getInt();
            case "FLOAT":
                return buf.getFloat();
            case "DOUBLE":
                return buf.getDouble();
            case "UINT16":
                return buf.getShort() & 0xFFFF;
            default:
                // 默认返回十六进制字符串
                return bytesToHex(slice);
        }
    }

    // ----------------------------------------------------------------
    // SCRIPT 解析：Groovy / JS 脚本
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseScript(String engineName, String scriptContent, String rawResponse) {
        if (engineName == null || scriptContent == null) {
            return Collections.emptyMap();
        }
        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName(engineName);
            if (engine == null) {
                throw new DeviceException("SCRIPT_ENGINE_NOT_FOUND", "脚本引擎不存在: " + engineName);
            }
            engine.put("raw", rawResponse);
            Object result = engine.eval(scriptContent);
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            return Map.of("result", result != null ? result : "");
        } catch (DeviceException e) {
            throw e;
        } catch (Exception e) {
            throw new DeviceException("SCRIPT_EVAL_FAILED", "脚本执行失败: " + e.getMessage(), e);
        }
    }

    // ----------------------------------------------------------------
    // 后处理
    // ----------------------------------------------------------------

    private void applyUnitConversions(Map<String, Object> result, String unitConversionsJson) {
        try {
            Map<String, UnitConversion> conversions = JSON.parseObject(unitConversionsJson,
                    new TypeReference<Map<String, UnitConversion>>() {});
            for (Map.Entry<String, UnitConversion> entry : conversions.entrySet()) {
                Object val = result.get(entry.getKey());
                if (val instanceof Number) {
                    double raw = ((Number) val).doubleValue();
                    UnitConversion conv = entry.getValue();
                    result.put(entry.getKey(), raw * conv.getFactor() + conv.getOffset());
                }
            }
        } catch (Exception e) {
            log.warn("[CodecExecutor] 单位换算失败: {}", e.getMessage());
        }
    }

    private void applyValidRange(Map<String, Object> result, String validRangeJson) {
        try {
            Map<String, ValidRange> ranges = JSON.parseObject(validRangeJson,
                    new TypeReference<Map<String, ValidRange>>() {});
            for (Map.Entry<String, ValidRange> entry : ranges.entrySet()) {
                Object val = result.get(entry.getKey());
                if (val instanceof Number) {
                    double v = ((Number) val).doubleValue();
                    ValidRange range = entry.getValue();
                    if ((range.getMin() != null && v < range.getMin())
                            || (range.getMax() != null && v > range.getMax())) {
                        log.warn("[CodecExecutor] 值超出有效范围 field={} value={} range=[{},{}]",
                                entry.getKey(), v, range.getMin(), range.getMax());
                        result.remove(entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[CodecExecutor] 有效范围校验失败: {}", e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // 工具方法 & 内部模型
    // ----------------------------------------------------------------

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Data
    private static class HexRule {
        private String fieldName;
        private int byteOffset;
        private int byteLength;
        private String dataType; // INT8/INT16/INT32/UINT16/FLOAT/DOUBLE
    }

    @Data
    private static class UnitConversion {
        private double factor = 1.0;
        private double offset = 0.0;
    }

    @Data
    private static class ValidRange {
        private Double min;
        private Double max;
    }
}
