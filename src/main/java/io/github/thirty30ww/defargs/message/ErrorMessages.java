package io.github.thirty30ww.defargs.message;

/**
 * 错误消息类
 * <p>
 * 统一管理所有错误相关的消息，包括类型不支持、注解冲突等错误信息
 */
public class ErrorMessages {
    private static final String PREFIX = "【DefaultValue】 ";

    /**
     * 不支持的参数类型错误
     *
     * @param paramType 参数类型
     * @param supportedTypes 支持的类型描述
     * @return 错误消息
     */
    public static String unsupportedParameterType(String paramType, String supportedTypes) {
        return PREFIX + "不支持的参数类型 '" + paramType + "'。支持的类型: " + supportedTypes;
    }

    /**
     * 参数不能同时使用 @DefaultValue 和 @Omittable
     *
     * @param paramName 参数名
     * @return 错误消息
     */
    public static String annotationsMutuallyExclusive(String paramName) {
        return PREFIX + "参数 '" + paramName + "' 不能同时使用 @DefaultValue 和 @Omittable 注解";
    }

    /**
     * @DefaultValue 不能用于抽象方法的错误
     *
     * @return 错误消息
     */
    public static String defaultValueOnAbstractMethod() {
        return PREFIX + "@DefaultValue 不能用于抽象方法。抽象方法请使用 @Omittable 注解";
    }

    /**
     * @Omittable 不能用于具体方法的错误
     *
     * @return 错误消息
     */
    public static String omittableOnConcreteMethod() {
        return PREFIX + "@Omittable 只能用于抽象方法。具体方法请使用 @DefaultValue 注解";
    }

    /**
     * 处理类时发生错误
     *
     * @param className 类名
     * @param error 错误信息
     * @return 错误消息
     */
    public static String processError(String className, String error) {
        return PREFIX + "处理类 " + className + " 时出错: " + error;
    }
}

