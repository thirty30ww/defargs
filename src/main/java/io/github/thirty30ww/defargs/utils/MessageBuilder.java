package io.github.thirty30ww.defargs.utils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * 消息构建器，统一管理所有编译消息
 * <p>
 * 所有消息都使用 "DefaultValue: " 前缀，保持一致的格式
 */
public class MessageBuilder {
    private static final String PREFIX = "【DefaultValue】 ";

    /**
     * 注解处理器初始化消息
     */
    public static String init() {
        return PREFIX + "注解处理器已初始化";
    }

    /**
     * 生成重载方法成功消息
     *
     * @param className 类名
     * @param count 生成的方法数量
     */
    public static String generatedMethods(String className, int count) {
        return PREFIX + "为类 " + className + " 生成了 " + count + " 个重载方法";
    }

    /**
     * 处理类时发生错误
     *
     * @param className 类名
     * @param error 错误信息
     */
    public static String processError(String className, String error) {
        return PREFIX + "处理类 " + className + " 时出错: " + error;
    }

    /**
     * 找不到方法的警告
     *
     * @param methodName 方法名
     */
    public static String methodNotFound(String methodName) {
        return PREFIX + "找不到方法: " + methodName;
    }

    /**
     * 只支持末尾连续默认参数的警告
     */
    public static String onlyTrailingDefaults() {
        return PREFIX + "目前只支持末尾连续的 @DefaultValue 参数";
    }

    /**
     * 重复方法定义错误（模仿 Java 原生错误格式）
     *
     * @param method 方法元素
     * @param paramCount 参数数量
     */
    public static String duplicateMethod(ExecutableElement method, int paramCount) {
        TypeElement classElement = (TypeElement) method.getEnclosingElement();
        String className = classElement.getQualifiedName().toString();

        // 构建参数类型列表
        StringBuilder paramTypes = new StringBuilder();
        var params = method.getParameters();
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) paramTypes.append(",");
            paramTypes.append(params.get(i).asType().toString());
        }

        return PREFIX + "已在类 " + className + " 中定义了方法 " +
               method.getSimpleName() + "(" + paramTypes + ")";
    }
}

