package io.github.thirty30ww.defargs.message;

/**
 * 信息消息类
 * <p>
 * 统一管理所有提示性信息，包括初始化、成功生成等信息
 */
public class InfoMessages {
    private static final String PREFIX = "【DefaultValue】 ";

    /**
     * 注解处理器初始化消息
     *
     * @return 初始化消息
     */
    public static String init() {
        return PREFIX + "注解处理器已初始化";
    }

    /**
     * 生成重载方法成功消息
     *
     * @param className 类名
     * @param count 生成的方法数量
     * @return 成功消息
     */
    public static String generatedMethods(String className, int count) {
        return PREFIX + "为类 " + className + " 生成了 " + count + " 个重载方法";
    }
}

