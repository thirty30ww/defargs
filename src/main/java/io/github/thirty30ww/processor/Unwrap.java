package io.github.thirty30ww.processor;

import javax.annotation.processing.ProcessingEnvironment;
import java.lang.reflect.Method;

/**
 * IDE 包装对象解包工具
 * <p>
 * IntelliJ IDEA 会包装注解处理器的 API 对象，导致无法转换为 Javac 内部类型。
 * 此工具通过反射调用 IDEA 的 {@code APIWrappers.unwrap()} 方法来恢复原始对象。
 * <p>
 * 示例：
 * <pre>
 * {@code
 * ProcessingEnvironment unwrapped = Unwrap.processingEnv(processingEnv);
 * JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) unwrapped; // ✓ 成功
 * }
 * </pre>
 *
 * @see Permit
 */
@SuppressWarnings("unchecked")
public class Unwrap {
    private Unwrap() {}

    /**
     * 解包 IntelliJ IDEA 包装的 ProcessingEnvironment
     * <p>
     * 示例：
     * <pre>
     * {@code
     * @Override
     * public void init(ProcessingEnvironment processingEnv) {
     *     super.init(processingEnv);
     *     
     *     ProcessingEnvironment unwrapped = Unwrap.processingEnv(processingEnv);
     *     Trees trees = Trees.instance(unwrapped);
     *     Context context = ((JavacProcessingEnvironment) unwrapped).getContext();
     * }
     * }
     * </pre>
     *
     * @param processingEnv 可能被包装的 ProcessingEnvironment
     * @return 解包后的对象（IDEA 环境），或原对象（其他环境）
     */
    public static ProcessingEnvironment processingEnv(ProcessingEnvironment processingEnv) {
        return unwrap(ProcessingEnvironment.class, processingEnv);
    }

    /**
     * 通用解包方法
     * <p>
     * 示例：
     * <pre>
     * {@code
     * Filer unwrappedFiler = Unwrap.unwrap(Filer.class, processingEnv.getFiler());
     * Elements unwrappedElements = Unwrap.unwrap(Elements.class, processingEnv.getElementUtils());
     * }
     * </pre>
     *
     * @param <T> 对象类型
     * @param iface 对象的接口 Class
     * @param wrapper 可能被包装的对象
     * @return 解包后的对象，或原对象
     */
    public static <T> T unwrap(Class<? extends T> iface, T wrapper) {
        if (wrapper == null) {
            return null;
        }

        try {
            // 通过 IDEA 的 APIWrappers 解包
            ClassLoader classLoader = wrapper.getClass().getClassLoader();
            Class<?> apiWrappers = classLoader.loadClass("org.jetbrains.jps.javac.APIWrappers");
            Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            T unwrapped = (T) unwrapMethod.invoke(null, iface, wrapper);
            return unwrapped != null ? unwrapped : wrapper;
        } catch (Throwable ignored) {
            // 非 IDEA 环境，直接返回原对象
            return wrapper;
        }
    }

    /**
     * 检查对象是否被 IDEA 包装（用于调试）
     * <p>
     * 示例：
     * <pre>
     * {@code
     * if (Unwrap.isWrapped(processingEnv)) {
     *     messager.printMessage(Diagnostic.Kind.NOTE, "检测到 IDEA 环境");
     * }
     * }
     * </pre>
     *
     * @param obj 要检查的对象
     * @return 是否被包装
     */
    public static boolean isWrapped(Object obj) {
        if (obj == null) {
            return false;
        }
        
        String className = obj.getClass().getName();
        return className.startsWith("org.jetbrains.jps.") 
            || className.startsWith("com.intellij.compiler.");
    }
}

