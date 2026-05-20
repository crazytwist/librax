package com.librax.lab.framework.common.util.expression;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一表达式工具
 *
 * <p>基于 Aviator 表达式引擎，提供模板渲染和布尔表达式求值能力。
 * 替换系统中散落的 ad-hoc 表达式解析（手动 ${} 替换、split("==") 等）。
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>模板渲染</b>：{@link #render(String, Map)} — 替换字符串中的 ${key} 占位符
 *   <li><b>布尔判断</b>：{@link #evalBool(String, Map)} — 执行布尔表达式，返回 true/false
 *   <li><b>通用求值</b>：{@link #eval(String, Map)} — 执行任意表达式，返回 Object
 * </ul>
 *
 * <h3>表达式示例</h3>
 * <pre>
 * // 模板渲染
 * render("${greeting}, ${name}!", Map.of("greeting", "Hello", "name", "World"))
 * // → "Hello, World!"
 *
 * // 布尔判断（支持数值/字符串比较、逻辑运算）
 * evalBool("status == 2", Map.of("status", 2))           // → true
 * evalBool("status >= 2 && status < 5", Map.of("status", 3))  // → true
 * evalBool("result == 'DONE'", Map.of("result", "DONE")) // → true
 * </pre>
 *
 * <p>注意：对于 JSONPath 查询（如 {@code $.status}），仍需使用 FastJSON 的 JSONPath.eval()
 * 提取值后，将值作为变量传入本工具类进行表达式判断。
 *
 * @author librax
 */
public final class ExpressionUtil {

    private static final ConcurrentHashMap<String, Expression> CACHE = new ConcurrentHashMap<>();

    private ExpressionUtil() {}

    // ================================================================
    // 模板渲染
    // ================================================================

    /**
     * 渲染模板字符串，替换 ${key} 占位符为 vars 中对应的值。
     *
     * <p>实现为纯字符串替换，不做表达式解析。原因是模板可能是 JSON、XML 等任意格式，
     * 其中的 {@code {}"} 等字符会被 Aviator 当成语法结构而报错。
     *
     * @param template 模板字符串，如 "hello, ${name}" 或 {"key": "${value}"}
     * @param vars     变量映射
     * @return 渲染后的字符串
     */
    public static String render(String template, Map<String, Object> vars) {
        if (template == null) return null;
        if (vars == null || vars.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            result = result.replace(
                    "${" + entry.getKey() + "}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }

    // ================================================================
    // 布尔判断
    // ================================================================

    /**
     * 执行布尔表达式（安全版本，异常时返回 false）。
     *
     * <p>内部做了两层保护：
     * <ol>
     *   <li>表达式为空 → false
     *   <li>执行异常 → 吞掉异常，返回 false
     * </ol>
     *
     * @param expr 布尔表达式，如 "status == 2"、"$.status == 'DONE'"
     * @param vars 变量映射
     * @return 表达式执行结果，异常时返回 false
     */
    public static boolean evalBool(String expr, Map<String, Object> vars) {
        if (expr == null || expr.isEmpty()) return false;
        try {
            Expression compiled = CACHE.computeIfAbsent("bool:" + expr, k ->
                    AviatorEvaluator.compile(expr));
            Object result = compiled.execute(vars != null ? vars : Collections.emptyMap());
            if (result instanceof Boolean b) return b;
            // 非布尔结果：例如 "$.completed" 返回了字符串 "true"
            return "true".equalsIgnoreCase(String.valueOf(result));
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================
    // 通用求值
    // ================================================================

    /**
     * 执行任意表达式，返回原始结果。
     *
     * @param expr 表达式
     * @param vars 变量映射
     * @return 表达式执行结果
     * @throws RuntimeException 如果表达式语法错误或执行异常
     */
    public static Object eval(String expr, Map<String, Object> vars) {
        Expression compiled = CACHE.computeIfAbsent("eval:" + expr, k ->
                AviatorEvaluator.compile(expr));
        return compiled.execute(vars != null ? vars : Collections.emptyMap());
    }

    /**
     * 清除编译缓存。主要用于测试或动态加载新表达式后刷新。
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
