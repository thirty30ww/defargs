package io.github.thirty30ww.defargs.message;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * 警告消息类
 * <p>
 * 统一管理所有警告相关的消息，包括方法未找到、不支持的参数位置等警告信息
 */
public class WarningMessages {
    private static final String PREFIX = "【DefaultValue】 ";

    /**
     * 找不到方法的警告
     *
     * @param methodName 方法名
     * @return 警告消息
     */
    public static String methodNotFound(String methodName) {
        return PREFIX + "找不到方法: " + methodName;
    }

    /**
     * 只支持末尾连续默认参数的警告
     *
     * @return 警告消息
     */
    public static String onlyTrailingDefaults() {
        return PREFIX + "目前只支持末尾连续的 @DefaultValue 或 @Omittable 参数";
    }

    /**
     * 重复方法定义警告（模仿 Java 原生错误格式）
     *
     * @param method 方法元素
     * @param paramCount 参数数量
     * @return 警告消息
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

