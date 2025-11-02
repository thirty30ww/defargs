package io.github.thirty30ww.defargs.utils;

import io.github.thirty30ww.defargs.annotation.DefaultValue;
import io.github.thirty30ww.defargs.annotation.Omittable;
import io.github.thirty30ww.defargs.message.ErrorMessages;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.*;

/**
 * 参数分析工具类
 * <p>
 * 分析方法参数，识别带 @DefaultValue 或 @Omittable 注解的参数，区分末尾连续的可省略参数（可生成重载）和中间的可省略参数（不可生成重载）。
 * <p>
 * 注解区别：
 * <ul>
 *   <li>{@code @DefaultValue}：用于具体方法，生成带默认值的重载方法</li>
 *   <li>{@code @Omittable}：用于抽象方法，生成抽象的重载方法</li>
 * </ul>
 * <p>
 * 示例：
 * <pre>
 * {@code
 * // 具体方法：可以生成重载（末尾连续）
 * void foo(String a, @DefaultValue("1") int b, @DefaultValue("2") int c)
 * -> 生成：foo(String a, int b) { foo(a, b, 2); }
 * 
 * // 抽象方法：生成抽象重载
 * abstract void bar(String a, @Omittable int b, @Omittable int c);
 * -> 生成：abstract void bar(String a, int b);
 * 
 * // 不能生成重载（中间有可省略参数）
 * void baz(String a, @DefaultValue("1") int b, String c)
 * -> b 的值无法确定
 * }
 * </pre>
 */
public class ParameterAnalyzer {

    /**
     * 参数分析结果记录类
     * 包含默认参数索引、默认参数值映射和末尾连续默认参数索引的记录类
     *
     * @param defaultIdxs       所有带默认值参数的索引列表，例如 [1, 3, 4]
     * @param defaultValueMap   带默认值参数的索引到默认值的映射，例如 {1="default1", 3="default3", 4="default4"}
     * @param trailingDefaults  末尾连续默认参数的索引列表，例如 [3, 4]
     */
    public record Result(List<Integer> defaultIdxs, Map<Integer, String> defaultValueMap,
                         List<Integer> trailingDefaults) {
        /**
         * 是否存在默认参数
         *
         * @return 如果存在默认参数则返回 true，否则返回 false
         */
        public boolean hasDefaults() {
            return !defaultIdxs.isEmpty();
        }

        /**
         * 是否存在末尾连续默认参数
         *
         * @return 如果存在末尾连续默认参数则返回 true，否则返回 false
         */
        public boolean hasTrailingDefaults() {
            return !trailingDefaults.isEmpty();
        }
    }

    /**
     * 分析方法的参数，找出带 @DefaultValue 或 @Omittable 注解的参数，例如
     * <p>
     * <pre>
     * {@code
     * public void setConfig(
     *     String host,                         // 索引 0
     *     @DefaultValue("8080") int port,      // 索引 1
     *     @DefaultValue("false") boolean ssl   // 索引 2
     * )
     * }
     * </pre>
     * 输出：
     * <pre>
     * {@code
     * Result(
     *     defaultIdxs=[1, 2],
     *     defaultValueMap={1="8080", 2="false"},
     *     trailingDefaults=[1, 2]
     * )
     * }
     * </pre>
     *
     * @param method 要分析的方法元素
     * @return 包含可省略参数索引、默认参数值映射和末尾连续可省略参数索引的结果记录类
     * @throws IllegalArgumentException 如果参数同时使用了 @DefaultValue 和 @Omittable，或参数类型不支持
     */
    public Result analyzeParameters(ExecutableElement method) {
        List<? extends VariableElement> parameters = method.getParameters();

        List<Integer> defaultIdxs = new ArrayList<>();
        Map<Integer, String> defaultValueMap = new HashMap<>();

        // 遍历参数，找出带 @DefaultValue 或 @Omittable 注解的参数
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            DefaultValue defaultValueAnnotation = param.getAnnotation(DefaultValue.class);
            Omittable omittableAnnotation = param.getAnnotation(Omittable.class);

            // 处理 @DefaultValue 注解
            if (defaultValueAnnotation != null) {
                String paramType = param.asType().toString();
                // 检查参数类型是否支持默认值转换
                if (!TypeConverter.isSupportedType(paramType)) {
                    throw new IllegalArgumentException(
                            ErrorMessages.unsupportedParameterType(paramType, 
                                    TypeConverter.getSupportedTypesDescription()));
                }
                defaultIdxs.add(i);
                defaultValueMap.put(i, defaultValueAnnotation.value());
            }
            
            // 处理 @Omittable 注解
            if (omittableAnnotation != null) {
                defaultIdxs.add(i);
                // @Omittable 不需要默认值，存储 null 作为标记
                defaultValueMap.put(i, null);
            }
        }

        // 提取末尾连续的可省略参数
        List<Integer> trailingDefaults = extractTrailingDefaults(defaultIdxs, parameters.size());

        return new Result(defaultIdxs, defaultValueMap, trailingDefaults);
    }

    /**
     * 提取末尾连续的默认参数索引，例如
     * <p>
     * <pre>
     * {@code
     * defaultIdxs = [1, 2, 3, 4]
     * totalParams = 5
     * }
     * </pre>
     * 输出：
     * <pre>
     * {@code
     * trailingDefaults = [3, 4]
     * }
     * </pre>
     *
     * @param defaultIdxs     带默认值参数的索引列表
     * @param totalParams     方法总参数数量
     * @return 末尾连续默认参数的索引列表
     */
    private List<Integer> extractTrailingDefaults(List<Integer> defaultIdxs, int totalParams) {
        List<Integer> trailing = new ArrayList<>();
        int expected = totalParams - 1;

        for (int i = defaultIdxs.size() - 1; i >= 0; i--) {
            if (defaultIdxs.get(i) == expected) {
                trailing.add(0, defaultIdxs.get(i));
                expected--;
            } else {
                break;
            }
        }

        return trailing;
    }
}

