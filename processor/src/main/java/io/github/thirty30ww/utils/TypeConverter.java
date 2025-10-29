package io.github.thirty30ww.utils;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;

import java.util.*;
import java.util.function.BiFunction;

/**
 * 类型转换工具类
 * <p>
 * 负责类型检查和字符串到 AST 字面量的转换。基于注册表模式，所有类型在 static 块中统一注册。
 * <p>
 * 核心设计：一个 TypeHandler 对应多个键（如 "int", "Integer", "java.lang.Integer" 都指向同一个处理器）
 * <p>
 * 示例：
 * <pre>
 * {@code
 * // 检查类型支持
 * TypeConverter.isSupportedType("int");  // true
 * 
 * // 创建字面量表达式
 * createDefaultValueExpression(treeMaker, "42", "int");
 * // -> treeMaker.Literal(42)
 * }
 * </pre>
 */
public class TypeConverter {

    /**
     * 类型处理器接口
     */
    private interface TypeHandler {
        /**
         * 解析字符串值为对应类型的字面量
         * @param treeMaker 树生成器
         * @param value 字符串值
         * @return 对应类型的字面量表达式
         */
        JCTree.JCExpression createLiteral(TreeMaker treeMaker, String value);
        
        /**
         * 获取类型的显示名称
         */
        String getDisplayName();
    }

    /**
     * 注册表：存储所有支持的类型及其对应的处理器，例如：
     * <p>
     * "int" -> IntegerHandler, "Integer" -> IntegerHandler, "java.lang.Integer" -> IntegerHandler
     */
    private static final Map<String, TypeHandler> TYPE_HANDLERS = new LinkedHashMap<>();
    
    /**
     * 基本类型到包装类的映射，例如：
     * <p>
     * "int" -> "java.lang.Integer"
     */
    private static final Map<String, String> PRIMITIVE_TO_WRAPPER = new HashMap<>();

    // 初始化静态块，注册所有支持的类型处理器
    static {
        registerType("String", "String",
            (tm, v) -> tm.Literal(v));
        
        registerNumericType("int", "Integer", 
            (tm, v) -> tm.Literal(Integer.parseInt(v)));
        
        registerNumericType("long", "Long", 
            (tm, v) -> tm.Literal(Long.parseLong(v)));
        
        registerNumericType("short", "Short", 
            (tm, v) -> tm.Literal(Short.parseShort(v)));
        
        registerNumericType("byte", "Byte", 
            (tm, v) -> tm.Literal(Byte.parseByte(v)));
        
        registerNumericType("float", "Float", 
            (tm, v) -> tm.Literal(Float.parseFloat(v)));
        
        registerNumericType("double", "Double", 
            (tm, v) -> tm.Literal(Double.parseDouble(v)));
        
        registerNumericType("boolean", "Boolean", 
            (tm, v) -> tm.Literal(Boolean.parseBoolean(v)));
        
        registerNumericType("char", "Character", 
            (tm, v) -> tm.Literal(v.charAt(0)));
    }

    /**
     * 注册类型处理器，例如
     * <p>
     * <code>registerType("String", "String", (tm, v) -> tm.Literal(v));</code>
     * <p>
     * 这会在 TYPE_HANDLERS 中注册 "String"、"String"（重复） 和 "java.lang.String" 三种类型，它们都指向同一个 StringHandler。
     *
     * @param primitiveOrSimpleName 基本类型或简单类名（如 "int" 或 "String"）
     * @param displayName 类型的显示名称（如 "Integer" 或 "String"）
     * @param literalCreator 用于创建字面量表达式的函数
     */
    private static void registerType(String primitiveOrSimpleName, String displayName, 
                                     BiFunction<TreeMaker, String, JCTree.JCExpression> literalCreator) {
        TypeHandler handler = new TypeHandler() {
            @Override
            public JCTree.JCExpression createLiteral(TreeMaker treeMaker, String value) {
                return literalCreator.apply(treeMaker, value);
            }
            
            @Override
            public String getDisplayName() {
                return displayName;
            }
        };
        
        TYPE_HANDLERS.put(primitiveOrSimpleName, handler);
        TYPE_HANDLERS.put(displayName, handler);
        TYPE_HANDLERS.put("java.lang." + displayName, handler);
    }
    
    /**
     * 注册数值类型（包含基本类型和包装类型），例如
     * <p>
     * <code>registerNumericType("int", "Integer", (tm, v) -> tm.Literal(Integer.parseInt(v)));</code>
     * <p>
     * 这会在 TYPE_HANDLERS 中注册 "int"、"Integer" 和 "java.lang.Integer" 三种类型，它们都指向同一个 IntegerHandler。
     * <p>
     * 同时在 PRIMITIVE_TO_WRAPPER 中注册基本类型到包装类的映射，例如 "int" -> "java.lang.Integer"。
     *
     * @param primitive 基本类型（如 "int"）
     * @param wrapper 包装类（如 "Integer"）
     * @param literalCreator 用于创建字面量表达式的函数
     */
    private static void registerNumericType(String primitive, String wrapper, 
                                           BiFunction<TreeMaker, String, JCTree.JCExpression> literalCreator) {
        registerType(primitive, wrapper, literalCreator);
        PRIMITIVE_TO_WRAPPER.put(primitive, "java.lang." + wrapper);
    }

    /**
     * 标准化类型名称，将基本类型转换为对应的包装类
     * <p>
     * 目的：统一类型表示，避免在后续处理中区分基本类型和包装类型。
     * <p>
     * 示例：
     * <ul>
     *   <li>"int" -> "java.lang.Integer"</li>
     *   <li>"Integer" -> "Integer"（已经是包装类，不变）</li>
     *   <li>"List<String>" -> "List"（移除泛型信息）</li>
     * </ul>
     * @param typeName 原始类型名称（如 "int" 或 "java.lang.Integer"）
     * @return 标准化后的类型名称（如 "java.lang.Integer"）
     */
    public static String normalizeTypeName(String typeName) {
        // 移除泛型信息
        if (typeName.contains("<")) {
            typeName = typeName.substring(0, typeName.indexOf("<"));
        }
        
        // 转换基本类型为包装类
        return PRIMITIVE_TO_WRAPPER.getOrDefault(typeName, typeName);
    }

    /**
     * 检查类型是否被支持
     * @param typeName 要检查的类型名称（如 "int" 或 "java.lang.Integer"）
     * @return 如果类型被支持则返回 true，否则返回 false
     */
    public static boolean isSupportedType(String typeName) {
        return TYPE_HANDLERS.containsKey(normalizeTypeName(typeName));
    }

    /**
     * 获取支持的类型列表（用于错误提示）
     * @return 包含所有支持类型显示名称的字符串，格式为 "Integer, Long, ... and their primitive equivalents"
     */
    public static String getSupportedTypesDescription() {
        Set<String> displayNames = new LinkedHashSet<>();
        for (TypeHandler handler : TYPE_HANDLERS.values()) {
            displayNames.add(handler.getDisplayName());
        }
        
        StringBuilder sb = new StringBuilder();
        for (String name : displayNames) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(name);
        }
        
        sb.append(" and their primitive equivalents");
        return sb.toString();
    }

    /**
     * 根据字符串和类型创建默认值表达式（AST 节点），例如
     * <p>
     * <pre>
     *  {@code
     *  // 代码：void foo(@DefaultValue("42") int x)
     *  createDefaultValueExpression(treeMaker, "42", "int");
     *  -> normalizeType = "Integer";
     *  -> handler = TYPE_HANDLERS.get("Integer");  // 获取到 IntegerHandler
     *  -> return = handler.createLiteral(treeMaker, "42");  // 创建整数 42 的 AST 节点
     *  }
     *  </pre>
     *
     * <p>
     * 这会返回一个表示整数 42 的 AST 节点。
     * @param treeMaker 树生成器
     * @param value 字符串值（如 "10" 或 "true"）
     * @param type 类型名称（如 "int" 或 "java.lang.Integer"）
     * @return 对应的默认值表达式 AST 节点，如果类型不支持或解析失败则返回 null
     */
    public static JCTree.JCExpression createDefaultValueExpression(TreeMaker treeMaker, String value, String type) {
        // 标准化类型名称
        String normalizedType = normalizeTypeName(type);
        
        // 移除包名前缀以匹配处理器
        normalizedType = normalizedType.replace("java.lang.", "");
        
        TypeHandler handler = TYPE_HANDLERS.get(normalizedType);
        if (handler != null) {
            try {
                return handler.createLiteral(treeMaker, value);
            } catch (Exception e) {
                // 解析失败时返回 null
                return treeMaker.Literal(null);
            }
        }
        
        // 不支持的类型返回 null
        return treeMaker.Literal(null);
    }
}

