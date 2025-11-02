package io.github.thirty30ww.defargs.utils;

import com.sun.tools.javac.tree.JCTree;
import io.github.thirty30ww.defargs.annotation.DefaultValue;
import io.github.thirty30ww.defargs.annotation.Omittable;
import io.github.thirty30ww.defargs.message.ErrorMessages;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * 注解分析工具类
 * <p>
 * 负责所有与 @DefaultValue 和 @Omittable 注解相关的分析和验证：
 * <ul>
 *   <li>从注解处理环境中收集带注解的方法</li>
 *   <li>验证注解使用规则（@DefaultValue 用于具体方法，@Omittable 用于抽象方法）</li>
 *   <li>验证参数不能同时使用两个注解</li>
 * </ul>
 * <p>
 * 示例：
 * <pre>
 * {@code
 * // 正确：具体方法使用 @DefaultValue
 * public void foo(int a, @DefaultValue("1") int b) { }
 * 
 * // 正确：抽象方法使用 @Omittable
 * abstract void bar(int a, @Omittable int b);
 * 
 * // 错误：抽象方法使用 @DefaultValue
 * abstract void baz(int a, @DefaultValue("1") int b);  // 抛出异常
 * 
 * // 错误：同时使用两个注解
 * public void qux(int a, @DefaultValue("1") @Omittable int b) { }  // 抛出异常
 * }
 * </pre>
 */
public class AnnotationAnalyzer {
    
    /**
     * 从注解处理环境中收集带有 @DefaultValue 或 @Omittable 注解的方法，并按类分组
     * <p>
     * 示例：
     * <pre>
     * {@code
     * class UserService {
     *     void save(@DefaultValue("admin") String name) { }
     * }
     * interface OrderService {
     *     void create(int id, @Omittable int count);
     * }
     * // 返回：{ UserService -> [save], OrderService -> [create] }
     * }
     * </pre>
     *
     * @param roundEnv 注解处理环境
     * @return 类元素到方法集合的映射
     */
    public static Map<TypeElement, Set<ExecutableElement>> collectAnnotatedMethods(RoundEnvironment roundEnv) {
        Map<TypeElement, Set<ExecutableElement>> classMethods = new HashMap<>();

        // 收集带 @DefaultValue 注解的方法
        collectMethodsWithAnnotation(roundEnv, DefaultValue.class, classMethods);

        // 收集带 @Omittable 注解的方法
        collectMethodsWithAnnotation(roundEnv, Omittable.class, classMethods);

        return classMethods;
    }
    
    /**
     * 收集带有指定注解的方法并按类分组
     * 
     * @param roundEnv 注解处理环境
     * @param annotationClass 要查找的注解类
     * @param classMethods 用于存储结果的映射（类 -> 方法集合）
     */
    private static void collectMethodsWithAnnotation(
            RoundEnvironment roundEnv,
            Class<? extends java.lang.annotation.Annotation> annotationClass,
            Map<TypeElement, Set<ExecutableElement>> classMethods) {
        
        for (Element element : roundEnv.getElementsAnnotatedWith(annotationClass)) {
            if (element.getKind() == ElementKind.PARAMETER) {
                Element enclosingElement = element.getEnclosingElement();
                if (enclosingElement instanceof ExecutableElement method) {
                    TypeElement classElement = (TypeElement) method.getEnclosingElement();
                    classMethods.computeIfAbsent(classElement, k -> new LinkedHashSet<>())
                            .add(method);
                }
            }
        }
    }
    
    /**
     * 验证方法参数注解的使用是否合法
     * <p>
     * 检查每个参数的注解使用是否符合以下规则：
     * <ul>
     *   <li>@DefaultValue 只能用于有方法体的方法（具体方法）</li>
     *   <li>@Omittable 只能用于抽象方法</li>
     *   <li>两个注解不能同时使用</li>
     * </ul>
     * 
     * @param method 要验证的方法
     * @param methodTree 方法的 AST 节点（用于判断是否是抽象方法）
     * @throws IllegalArgumentException 如果注解使用不合法
     */
    public static void validateAnnotations(ExecutableElement method, JCTree.JCMethodDecl methodTree) {
        boolean isAbstractMethod = methodTree.body == null;
        
        for (VariableElement param : method.getParameters()) {
            boolean hasDefaultValue = param.getAnnotation(DefaultValue.class) != null;
            boolean hasOmittable = param.getAnnotation(Omittable.class) != null;
            
            // 检查互斥：不能同时使用两个注解
            if (hasDefaultValue && hasOmittable) {
                throw new IllegalArgumentException(
                        ErrorMessages.annotationsMutuallyExclusive(param.getSimpleName().toString()));
            }
            
            // @DefaultValue 只能用于有方法体的方法
            if (hasDefaultValue && isAbstractMethod) {
                throw new IllegalArgumentException(ErrorMessages.defaultValueOnAbstractMethod());
            }
            
            // @Omittable 只能用于抽象方法
            if (hasOmittable && !isAbstractMethod) {
                throw new IllegalArgumentException(ErrorMessages.omittableOnConcreteMethod());
            }
        }
    }
}

