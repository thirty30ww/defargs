package io.github.thirty30ww.defargs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解用于标记抽象方法的参数为可省略参数。
 * 
 * <p>与 {@link DefaultValue} 的区别：
 * <ul>
 *   <li>{@code @Omittable}：用于接口或抽象类的抽象方法，生成抽象的重载方法</li>
 *   <li>{@code @DefaultValue}：用于具体类的方法，生成带默认值的重载方法</li>
 * </ul>
 * 
 * <p>示例（接口）：
 * <pre>{@code
 * public interface TestService {
 *     // 抽象方法，使用 @Omittable
 *     int test(int a, @Omittable int b);
 *     // 编译后自动生成抽象重载：int test(int a);
 * }
 * }</pre>
 * 
 * <p>示例（实现类）：
 * <pre>{@code
 * public class TestServiceImpl implements TestService {
 *     @Override
 *     public int test(int a, @DefaultValue("2") int b) {
 *         return a + b;
 *     }
 *     // 编译后自动生成带实现的重载：public int test(int a) { return test(a, 2); }
 * }
 * }</pre>
 * 
 * <p>约束：
 * <ul>
 *   <li>{@code @Omittable} 只能用于抽象方法（没有方法体的方法）</li>
 *   <li>{@code @DefaultValue} 只能用于具体方法（有方法体的方法）</li>
 *   <li>同一个参数不能同时使用这两个注解</li>
 *   <li>可省略的参数必须从右向左连续</li>
 * </ul>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Omittable {
}

