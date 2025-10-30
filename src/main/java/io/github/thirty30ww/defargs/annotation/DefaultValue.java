package io.github.thirty30ww.defargs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解用于为方法参数指定默认值。
 *
 * 简化实现说明：
 * - 使用 CLASS 级别的保留策略，便于在注解处理器中通过 element.getAnnotation 直接读取值；
 * - 仅用于编译期代码生成，不需要在运行时保留到反射。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface DefaultValue {
    /**
     * 默认值的字符串表示。
     */
    String value();
}
