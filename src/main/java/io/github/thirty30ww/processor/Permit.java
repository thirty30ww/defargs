package io.github.thirty30ww.processor;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反射权限绕过工具类（Lombok 风格）
 * <p>
 * 用于在 JDK 9+ 模块系统下绕过反射访问限制。通过 Unsafe 直接操作内存，
 * 修改 AccessibleObject 的 override 字段，实现真正的"强制访问"。
 * <p>
 * 核心技术：
 * <ul>
 *   <li>Unsafe：JVM 底层 API，可以直接操作内存</li>
 *   <li>字段偏移量：定位对象字段在内存中的位置</li>
 *   <li>override 字段：控制反射是否检查访问权限</li>
 * </ul>
 * <p>
 * 示例：
 * <pre>
 * {@code
 * // 传统方式（JDK 9+ 可能失败）
 * method.setAccessible(true);  // 可能抛出 InaccessibleObjectException
 * 
 * // Permit 方式（绕过限制）
 * Permit.setAccessible(method);  // 通过 Unsafe 强制设置，不会失败
 * }
 * </pre>
 */
@SuppressWarnings({"sunapi", "all"})
public class Permit {
    private Permit() {}

    /**
     * AccessibleObject.override 字段在内存中的偏移量
     * <p>
     * 用于通过 Unsafe 直接修改该字段，绕过访问检查
     */
    private static final long ACCESSIBLE_OVERRIDE_FIELD_OFFSET;
    
    /**
     * Unsafe 实例，用于直接操作内存
     */
    private static final Unsafe UNSAFE;

    static {
        // 初始化 Unsafe 实例
        UNSAFE = getUnsafeInstance();
        long offset = -1L;
        
        try {
            if (UNSAFE != null) {
                // 获取 override 字段的内存偏移量
                offset = getOverrideFieldOffset();
            }
        } catch (Throwable t) {
            // 如果失败，回退到传统的 setAccessible 方式
        }
        
        ACCESSIBLE_OVERRIDE_FIELD_OFFSET = offset;
    }

    /**
     * 获取 Unsafe 实例
     * <p>
     * Unsafe 是 JVM 底层 API，可以直接操作内存、绕过安全检查。
     * 它被隐藏在 sun.misc 包中，需要通过反射获取。
     * <p>
     * 执行过程：
     * <pre>
     * {@code
     * // 1. 获取 Unsafe.theUnsafe 静态字段
     * Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
     * 
     * // 2. 强制访问（这是唯一一次需要用传统 setAccessible 的地方）
     * theUnsafe.setAccessible(true);
     * 
     * // 3. 获取 Unsafe 实例
     * Unsafe unsafe = (Unsafe) theUnsafe.get(null);
     * }
     * </pre>
     *
     * @return Unsafe 实例，获取失败返回 null
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
     * 获取 AccessibleObject.override 字段的内存偏移量
     * <p>
     * override 字段控制反射是否检查访问权限：
     * <ul>
     *   <li>override = true：跳过访问检查</li>
     *   <li>override = false：执行访问检查</li>
     * </ul>
     * <p>
     * 为什么需要偏移量？
     * <pre>
     * {@code
     * // 对象在内存中的布局（简化）：
     * [对象头 | override字段 | 其他字段...]
     *         ↑
     *      偏移量 = 12（假设）
     * 
     * // 有了偏移量，Unsafe 可以直接修改该字段：
     * unsafe.putBoolean(method, 12, true);  // 直接将 override 设为 true
     * }
     * </pre>
     * <p>
     * JDK 版本兼容：
     * <ul>
     *   <li>JDK 9-11：可以直接反射获取 override 字段</li>
     *   <li>JDK 12+：反射被限制，使用伪造类结构获取偏移量</li>
     * </ul>
     *
     * @return override 字段的内存偏移量
     */
    private static long getOverrideFieldOffset() throws Throwable {
        try {
            // 尝试直接获取（JDK 9-11）
            Field f = AccessibleObject.class.getDeclaredField("override");
            return UNSAFE.objectFieldOffset(f);
        } catch (Throwable t) {
            // JDK 12+ 无法直接访问，使用伪造类（字段布局相同）
            return UNSAFE.objectFieldOffset(Fake.class.getDeclaredField("override"));
        }
    }

    /**
     * 伪造的 AccessibleObject 字段布局
     * <p>
     * 原理：只要字段顺序和类型相同，偏移量就相同
     * <pre>
     * {@code
     * // AccessibleObject 的实际字段（简化）：
     * class AccessibleObject {
     *     boolean override;        // 第一个字段
     *     Object accessCheckCache; // 第二个字段
     * }
     * 
     * // 伪造的类（字段顺序和类型完全相同）：
     * class Fake {
     *     boolean override;        // 第一个字段，偏移量相同
     *     Object accessCheckCache; // 第二个字段
     * }
     * 
     * // 因此可以用 Fake.override 的偏移量代替 AccessibleObject.override
     * }
     * </pre>
     */
    static class Fake {
        boolean override;
        Object accessCheckCache;
    }

    /**
     * 强制设置反射对象为可访问（绕过 JDK 9+ 限制）
     * <p>
     * 对比传统方式：
     * <pre>
     * {@code
     * // 传统方式（JDK 9+ 可能失败）
     * Method m = SomeClass.class.getDeclaredMethod("privateMethod");
     * m.setAccessible(true);  // 可能抛出 InaccessibleObjectException
     * 
     * // Permit 方式（强制成功）
     * Method m = SomeClass.class.getDeclaredMethod("privateMethod");
     * Permit.setAccessible(m);  // 通过 Unsafe 直接修改内存，必定成功
     * m.invoke(obj);  // 可以调用了
     * }
     * </pre>
     * <p>
     * 工作原理：
     * <pre>
     * {@code
     * // 1. 通过 Unsafe 直接修改对象内存
     * unsafe.putBoolean(method, offset, true);
     * 
     * // 等价于（如果允许的话）：
     * method.override = true;
     * 
     * // 效果：绕过所有访问检查
     * }
     * </pre>
     *
     * @param accessor 反射对象（Method、Field、Constructor 等）
     * @return 同一个对象（已设置为可访问）
     */
    public static <T extends AccessibleObject> T setAccessible(T accessor) {
        if (UNSAFE != null && ACCESSIBLE_OVERRIDE_FIELD_OFFSET != -1) {
            // 使用 Unsafe 直接修改 override 字段（强制方式）
            UNSAFE.putBoolean(accessor, ACCESSIBLE_OVERRIDE_FIELD_OFFSET, true);
        } else {
            // 回退到传统方式
            accessor.setAccessible(true);
        }
        return accessor;
    }

    /**
     * 获取方法（包括私有方法和父类方法）并强制设为可访问
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 获取私有方法
     * Method m = Permit.getMethod(String.class, "toUpperCase");
     * m.invoke("hello");  // 可以调用
     * }
     * </pre>
     *
     * @param c 目标类
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 已设为可访问的方法对象
     */
    public static Method getMethod(Class<?> c, String methodName, Class<?>... parameterTypes) 
            throws NoSuchMethodException {
        Method m = null;
        Class<?> current = c;
        
        // 在类继承链中查找方法
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
     * 获取字段（包括私有字段和父类字段）并强制设为可访问
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 获取私有字段
     * Field f = Permit.getField(String.class, "value");
     * char[] value = (char[]) f.get("hello");  // 可以访问
     * }
     * </pre>
     *
     * @param c 目标类
     * @param fieldName 字段名
     * @return 已设为可访问的字段对象
     */
    public static Field getField(Class<?> c, String fieldName) throws NoSuchFieldException {
        Field f = null;
        Class<?> current = c;
        
        // 在类继承链中查找字段
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
     * <p>
     * 供 ModuleAccessor 使用，用于模块破解。
     *
     * @return Unsafe 实例
     */
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }

    /**
     * 获取布尔字段的偏移量
     * <p>
     * 用于 ModuleAccessor 中修改 Method.accessible 字段。
     * <p>
     * 原理：
     * <pre>
     * {@code
     * // 任何类的第一个布尔字段的偏移量都相同（在相同 JVM 中）
     * class Parent {
     *     boolean first;  // 偏移量固定（如 12）
     * }
     * 
     * // 可以用这个偏移量修改任何对象的第一个布尔字段
     * }
     * </pre>
     *
     * @param unsafe Unsafe 实例
     * @return 布尔字段的偏移量
     */
    public static long getFirstFieldOffset(Unsafe unsafe) {
        try {
            return unsafe.objectFieldOffset(Parent.class.getDeclaredField("first"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 辅助类，用于获取布尔字段的标准偏移量
     */
    static class Parent {
        boolean first;
    }
}

