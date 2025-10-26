package io.github.thirty30ww.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 类型转换工具类
 * 负责将字符串值转换为对应 Java 类型的源代码字面量
 */
public class TypeConversionUtils {

    // 基本类型映射表
    private static final Map<String, String> PRIMITIVE_TYPE_MAP = new HashMap<>();
    
    static {
        // 基本类型的包装类映射
        PRIMITIVE_TYPE_MAP.put("int", "java.lang.Integer");
        PRIMITIVE_TYPE_MAP.put("long", "java.lang.Long");
        PRIMITIVE_TYPE_MAP.put("float", "java.lang.Float");
        PRIMITIVE_TYPE_MAP.put("double", "java.lang.Double");
        PRIMITIVE_TYPE_MAP.put("boolean", "java.lang.Boolean");
        PRIMITIVE_TYPE_MAP.put("byte", "java.lang.Byte");
        PRIMITIVE_TYPE_MAP.put("short", "java.lang.Short");
        PRIMITIVE_TYPE_MAP.put("char", "java.lang.Character");
    }

    /**
     * 将字符串值转换为对应 Java 类型的源代码字面量
     *
     * @param parameterType 参数类型的完整名称
     * @param rawValue      原始字符串值
     * @return 转换后的源代码字面量
     * @throws IllegalArgumentException 如果类型不支持或值格式不正确
     */
    public static String toSourceLiteral(String parameterType, String rawValue) {
        if (rawValue == null) {
            return "null";
        }

        // 标准化类型名称（处理基本类型）
        String normalizedType = normalizeTypeName(parameterType);
        
        try {
            return convertByType(normalizedType, rawValue);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Cannot convert value '" + rawValue + "' to type '" + parameterType + "': " + e.getMessage(), e);
        }
    }

    /**
     * 标准化类型名称，将基本类型转换为对应的包装类
     */
    private static String normalizeTypeName(String typeName) {
        // 移除泛型信息
        if (typeName.contains("<")) {
            typeName = typeName.substring(0, typeName.indexOf("<"));
        }
        
        // 转换基本类型为包装类
        return PRIMITIVE_TYPE_MAP.getOrDefault(typeName, typeName);
    }

    /**
     * 根据类型进行具体的转换
     */
    private static String convertByType(String typeName, String rawValue) {
        switch (typeName) {
            // 字符串类型
            case "java.lang.String":
            case "String":
                return convertToStringLiteral(rawValue);
                
            // 整数类型
            case "java.lang.Integer":
            case "Integer":
                return convertToIntegerLiteral(rawValue);
                
            case "java.lang.Long":
            case "Long":
                return convertToLongLiteral(rawValue);
                
            case "java.lang.Short":
            case "Short":
                return convertToShortLiteral(rawValue);
                
            case "java.lang.Byte":
            case "Byte":
                return convertToByteLiteral(rawValue);
                
            // 浮点类型
            case "java.lang.Float":
            case "Float":
                return convertToFloatLiteral(rawValue);
                
            case "java.lang.Double":
            case "Double":
                return convertToDoubleLiteral(rawValue);
                
            // 布尔类型
            case "java.lang.Boolean":
            case "Boolean":
                return convertToBooleanLiteral(rawValue);
                
            // 字符类型
            case "java.lang.Character":
            case "Character":
                return convertToCharacterLiteral(rawValue);
                
            default:
                // 对于不支持的类型，尝试直接返回原值（可能是枚举或其他常量）
                return rawValue;
        }
    }

    /**
     * 转换为字符串字面量
     */
    private static String convertToStringLiteral(String rawValue) {
        String escaped = rawValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    /**
     * 转换为整数字面量
     */
    private static String convertToIntegerLiteral(String rawValue) {
        Integer.parseInt(rawValue); // 验证格式
        return rawValue;
    }

    /**
     * 转换为长整数字面量
     */
    private static String convertToLongLiteral(String rawValue) {
        Long.parseLong(rawValue); // 验证格式
        return rawValue + "L";
    }

    /**
     * 转换为短整数字面量
     */
    private static String convertToShortLiteral(String rawValue) {
        Short.parseShort(rawValue); // 验证格式
        return "(short)" + rawValue;
    }

    /**
     * 转换为字节字面量
     */
    private static String convertToByteLiteral(String rawValue) {
        Byte.parseByte(rawValue); // 验证格式
        return "(byte)" + rawValue;
    }

    /**
     * 转换为浮点数字面量
     */
    private static String convertToFloatLiteral(String rawValue) {
        Float.parseFloat(rawValue); // 验证格式
        return rawValue + "f";
    }

    /**
     * 转换为双精度浮点数字面量
     */
    private static String convertToDoubleLiteral(String rawValue) {
        Double.parseDouble(rawValue); // 验证格式
        return rawValue + "d";
    }

    /**
     * 转换为布尔字面量
     */
    private static String convertToBooleanLiteral(String rawValue) {
        String lowerValue = rawValue.toLowerCase().trim();
        if ("true".equals(lowerValue)) {
            return "true";
        } else if ("false".equals(lowerValue)) {
            return "false";
        } else {
            throw new IllegalArgumentException("Boolean value must be 'true' or 'false', got: " + rawValue);
        }
    }

    /**
     * 转换为字符字面量
     */
    private static String convertToCharacterLiteral(String rawValue) {
        if (rawValue.length() == 1) {
            char c = rawValue.charAt(0);
            return "'" + escapeChar(c) + "'";
        } else {
            throw new IllegalArgumentException("Character value must be exactly one character, got: " + rawValue);
        }
    }

    /**
     * 转义字符
     */
    private static String escapeChar(char c) {
        switch (c) {
            case '\'': return "\\'";
            case '\\': return "\\\\";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\t': return "\\t";
            case '\b': return "\\b";
            case '\f': return "\\f";
            default: return String.valueOf(c);
        }
    }

    /**
     * 检查类型是否被支持
     *
     * @param typeName 类型名称
     * @return 如果类型被支持则返回 true
     */
    public static boolean isSupportedType(String typeName) {
        String normalizedType = normalizeTypeName(typeName);

        return switch (normalizedType) {
            case "java.lang.String", "String", "java.lang.Integer", "Integer", "java.lang.Long", "Long",
                 "java.lang.Short", "Short", "java.lang.Byte", "Byte", "java.lang.Float", "Float", "java.lang.Double",
                 "Double", "java.lang.Boolean", "Boolean", "java.lang.Character", "Character" -> true;
            default -> false;
        };
    }

    /**
     * 获取支持的类型列表（用于错误提示）
     */
    public static String getSupportedTypesDescription() {
        return "String, Integer, Long, Short, Byte, Float, Double, Boolean, Character " +
               "and their primitive equivalents (int, long, short, byte, float, double, boolean, char)";
    }
}