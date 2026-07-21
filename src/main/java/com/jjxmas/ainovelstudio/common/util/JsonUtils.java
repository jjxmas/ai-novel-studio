package com.jjxmas.ainovelstudio.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类，封装对象序列化和常用 JSON 反序列化。
 */
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 阻止工具类被实例化。
     */
    private JsonUtils() {
    }

    /**
     * 将对象序列化为 JSON 字符串。
     */
    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("JSON 序列化失败", exception);
        }
    }

    /**
     * 将 JSON 数组字符串解析为字符串列表。
     */
    public static List<String> toStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    /**
     * 将 JSON 对象字符串解析为 Map。
     */
    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
