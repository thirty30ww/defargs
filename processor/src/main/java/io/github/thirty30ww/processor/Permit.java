package io.github.thirty30ww.processor;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 工具类，用于在 Java 9+ 模块系统下绕过访问限制。
 * 借鉴自 Lombok 的实现。
 */
@SuppressWarnings({"sunapi", "all"})
public class Permit {
    private Permit() {}

    private static final long ACCESSIBLE_OVERRIDE_FIELD_OFFSET;
    private static final Unsafe UNSAFE;

    static {
        UNSAFE = getUnsafeInstance();
        long offset = -1L;
        
        try {
            if (UNSAFE != null) {
                offset = getOverrideFieldOffset();
            }
        } catch (Throwable t) {
            // 如果失败，回退到传统的 setAccessible 方式
        }
        
        ACCESSIBLE_OVERRIDE_FIELD_OFFSET = offset;
    }

    /**
     * 获取 Unsafe 实例
     */
    private static Unsafe getUnsafeInstance() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 AccessibleObject 的 override 字段偏移量
     */
    private static long getOverrideFieldOffset() throws Throwable {
        try {
            Field f = AccessibleObject.class.getDeclaredField("override");
            return UNSAFE.objectFieldOffset(f);
        } catch (Throwable t) {
            // JDK 12+ 可能无法直接访问，使用伪造类
            return UNSAFE.objectFieldOffset(Fake.class.getDeclaredField("override"));
        }
    }

    /**
     * 伪造的类结构，用于在 JDK 12+ 中获取字段偏移量
     */
    static class Fake {
        boolean override;
        Object accessCheckCache;
    }

    /**
     * 设置对象为可访问
     */
    public static <T extends AccessibleObject> T setAccessible(T accessor) {
        if (UNSAFE != null && ACCESSIBLE_OVERRIDE_FIELD_OFFSET != -1) {
            UNSAFE.putBoolean(accessor, ACCESSIBLE_OVERRIDE_FIELD_OFFSET, true);
        } else {
            accessor.setAccessible(true);
        }
        return accessor;
    }

    /**
     * 获取方法，包括父类中的方法
     */
    public static Method getMethod(Class<?> c, String methodName, Class<?>... parameterTypes) 
            throws NoSuchMethodException {
        Method m = null;
        Class<?> current = c;
        
        while (current != null) {
            try {
                m = current.getDeclaredMethod(methodName, parameterTypes);
                break;
            } catch (NoSuchMethodException e) {
                // 继续在父类中查找
            }
            current = current.getSuperclass();
        }
        
        if (m == null) {
            throw new NoSuchMethodException(c.getName() + "::" + methodName);
        }
        
        return setAccessible(m);
    }

    /**
     * 获取字段，包括父类中的字段
     */
    public static Field getField(Class<?> c, String fieldName) throws NoSuchFieldException {
        Field f = null;
        Class<?> current = c;
        
        while (current != null) {
            try {
                f = current.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                // 继续在父类中查找
            }
            current = current.getSuperclass();
        }
        
        if (f == null) {
            throw new NoSuchFieldException(c.getName() + "::" + fieldName);
        }
        
        return setAccessible(f);
    }

    /**
     * 获取 Unsafe 实例（公开方法）
     */
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }

    /**
     * 获取字段偏移量（用于模块访问）
     */
    public static long getFirstFieldOffset(Unsafe unsafe) {
        try {
            return unsafe.objectFieldOffset(Parent.class.getDeclaredField("first"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 辅助类，用于获取字段偏移量
     */
    static class Parent {
        boolean first;
    }
}

