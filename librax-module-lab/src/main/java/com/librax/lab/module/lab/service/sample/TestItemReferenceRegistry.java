
package com.librax.lab.module.lab.service.sample;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检测项参考范围注册表
 *
 * 简单实现：内存 Map。
 * 后续可改为从数据库表（如 lab_test_item_config）加载，支持动态配置。
 */
public class TestItemReferenceRegistry {

    private static final Map<String, TestItemReference> REGISTRY = new ConcurrentHashMap<>();

    static {
        // 水质检测常用参考范围
        register("PH", "酸碱度", "pH", new BigDecimal("6.5"), new BigDecimal("8.5"), "6.5-8.5");
        register("TURBIDITY", "浊度", "NTU", new BigDecimal("0"), new BigDecimal("5.0"), "≤5.0");
        register("COD", "化学需氧量", "mg/L", new BigDecimal("0"), new BigDecimal("40"), "≤40");
        register("BOD", "生化需氧量", "mg/L", new BigDecimal("0"), new BigDecimal("20"), "≤20");
        register("NH3_N", "氨氮", "mg/L", new BigDecimal("0"), new BigDecimal("1.5"), "≤1.5");
        register("TP", "总磷", "mg/L", new BigDecimal("0"), new BigDecimal("0.4"), "≤0.4");
        register("TN", "总氮", "mg/L", new BigDecimal("0"), new BigDecimal("1.5"), "≤1.5");
        // 可继续添加...
    }

    public static void register(String testItem, String testItemName,
                                String unit, BigDecimal low, BigDecimal high,
                                String referenceText) {
        REGISTRY.put(testItem.toUpperCase(), new TestItemReference(
                testItem, testItemName, unit, low, high, referenceText));
    }

    public static TestItemReference get(String testItem) {
        return REGISTRY.get(testItem != null ? testItem.toUpperCase() : null);
    }

    @Data
    public static class TestItemReference {
        private final String testItem;
        private final String testItemName;
        private final String unit;
        private final BigDecimal referenceLow;
        private final BigDecimal referenceHigh;
        private final String referenceText;
    }
}