package io.github.thirty30ww.utils;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;

import java.util.HashMap;
import java.util.Map;

/**
 * 类型转换工具类
 * 负责类型检查和类型转换相关功能
 */
public class TypeConverter {

    // 基本类型映射表
    private static final Map<String, String> PRIMITIVE_TYPE_MAP = new HashMap<>();
    
    static {
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
     * 标准化类型名称，将基本类型转换为对应的包装类
     */
    public static String normalizeTypeName(String typeName) {
        // 移除泛型信息
        if (typeName.contains("<")) {
            typeName = typeName.substring(0, typeName.indexOf("<"));
        }
        
        // 转换基本类型为包装类
        return PRIMITIVE_TYPE_MAP.getOrDefault(typeName, typeName);
    }

    /**
     * 检查类型是否被支持
     */
    public static boolean isSupportedType(String typeName) {
        String normalizedType = normalizeTypeName(typeName);

        return switch (normalizedType) {
            case "java.lang.String", "String", 
                 "java.lang.Integer", "Integer", 
                 "java.lang.Long", "Long",
                 "java.lang.Short", "Short", 
                 "java.lang.Byte", "Byte", 
                 "java.lang.Float", "Float", 
                 "java.lang.Double", "Double",
                 "java.lang.Boolean", "Boolean", 
                 "java.lang.Character", "Character" -> true;
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

    /**
     * 根据字符串和类型创建默认值表达式（AST 节点）
     */
    public static JCTree.JCExpression createDefaultValueExpression(TreeMaker treeMaker, String value, String type) {
        // 处理基本类型和包装类型
        type = type.replace("java.lang.", "");

        return switch (type) {
            case "String" -> treeMaker.Literal(value);
            case "int", "Integer" -> treeMaker.Literal(Integer.parseInt(value));
            case "long", "Long" -> treeMaker.Literal(Long.parseLong(value));
            case "double", "Double" -> treeMaker.Literal(Double.parseDouble(value));
            case "float", "Float" -> treeMaker.Literal(Float.parseFloat(value));
            case "boolean", "Boolean" -> treeMaker.Literal(Boolean.parseBoolean(value));
            case "char", "Character" -> treeMaker.Literal(value.charAt(0));
            default -> treeMaker.Literal(null);
        };
    }
}

