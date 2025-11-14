package com.jd.genie.sse;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;

/**
 * SseEventBuilder 结构测试
 */
public class SseEventBuilderTest {

    @Test
    public void analyzeSseEventBuilder() throws Exception {
        System.out.println("=== 分析 SseEventBuilder 结构 ===\n");

        // 创建一个完整的 SseEventBuilder
        SseEmitter.SseEventBuilder builder = SseEmitter.event()
                .id("test-id-123")
                .name("test-event")
                .data("Hello World")
                .comment("This is a comment")
                .reconnectTime(5000L);

        System.out.println("✅ SseEventBuilder 创建成功\n");

        // 分析 builder 类的结构
        Class<?> builderClass = builder.getClass();
        System.out.println("📋 Builder 类信息:");
        System.out.println("   - 完整类名: " + builderClass.getName());
        System.out.println("   - 简单类名: " + builderClass.getSimpleName());
        System.out.println("   - 父类: " + builderClass.getSuperclass().getName());
        System.out.println();

        // 列出所有字段
        System.out.println("📊 字段列表:");
        Field[] fields = builderClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(builder);

            System.out.println("   字段名: " + field.getName());
            System.out.println("     - 类型: " + field.getType().getName());
            System.out.println("     - 值: " + formatValue(value));
            System.out.println("     - 值类型: " + (value != null ? value.getClass().getName() : "null"));
            System.out.println();
        }

        // 调用 build() 方法分析返回结果
        System.out.println("\n🔨 调用 build() 方法:");
        Object buildResult = builder.build();
        System.out.println("   - 返回类型: " + buildResult.getClass().getName());

        if (buildResult instanceof java.util.Set) {
            java.util.Set<?> set = (java.util.Set<?>) buildResult;
            System.out.println("   - Set 大小: " + set.size());

            int index = 0;
            for (Object item : set) {
                System.out.println("\n   [" + index + "] 元素类型: " + item.getClass().getName());

                // 分析 DataWithMediaType 的字段
                if (item.getClass().getSimpleName().contains("DataWithMediaType")) {
                    analyzeDataWithMediaType(item);
                }
                index++;
            }
        }

        System.out.println("\n=== 分析完成 ===");
    }

    private void analyzeDataWithMediaType(Object dataWithMediaType) {
        try {
            Class<?> clazz = dataWithMediaType.getClass();
            System.out.println("     DataWithMediaType 字段:");

            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(dataWithMediaType);

                System.out.println("       - " + field.getName() + ": " +
                    field.getType().getSimpleName() + " = " + formatValue(value));
            }
        } catch (Exception e) {
            System.err.println("     ⚠️ 分析 DataWithMediaType 失败: " + e.getMessage());
        }
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        String str = value.toString();
        if (str.length() > 150) {
            return str.substring(0, 150) + "... (截断)";
        }

        // 如果包含换行符，显示转义后的形式
        if (str.contains("\n")) {
            return "\"" + str.replace("\n", "\\n").replace("\r", "\\r") + "\"";
        }

        return str;
    }
}
