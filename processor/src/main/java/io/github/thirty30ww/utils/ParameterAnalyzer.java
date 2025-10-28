package io.github.thirty30ww.utils;

import io.github.thirty30ww.annotation.DefaultValue;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.*;

/**
 * 参数分析工具类
 */
public class ParameterAnalyzer {

    /**
         * 参数分析结果
         */
        public record Result(List<Integer> defaultIdxs, Map<Integer, String> defaultValueMap,
                             List<Integer> trailingDefaults) {
        public boolean hasDefaults() {
            return !defaultIdxs.isEmpty();
        }

        public boolean hasTrailingDefaults() {
            return !trailingDefaults.isEmpty();
        }
        }
    
    /**
     * 分析方法的参数，找出带默认值的参数
     */
    public Result analyzeParameters(ExecutableElement method) {
        List<? extends VariableElement> parameters = method.getParameters();
        List<Integer> defaultIdxs = new ArrayList<>();
        Map<Integer, String> defaultValueMap = new HashMap<>();
        
        // 遍历参数，找出带默认值的参数
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement param = parameters.get(i);
            DefaultValue annotation = param.getAnnotation(DefaultValue.class);
            
            if (annotation != null) {
                String paramType = param.asType().toString();
                if (!TypeConverter.isSupportedType(paramType)) {
                    throw new IllegalArgumentException("不支持的参数类型 '" + paramType + "'");
                }
                defaultIdxs.add(i);
                defaultValueMap.put(i, annotation.value());
            }
        }
        
        // 提取末尾连续的默认参数
        List<Integer> trailingDefaults = extractTrailingDefaults(defaultIdxs, parameters.size());
        
        return new Result(defaultIdxs, defaultValueMap, trailingDefaults);
    }
    
    /**
     * 提取末尾连续的默认参数索引
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

