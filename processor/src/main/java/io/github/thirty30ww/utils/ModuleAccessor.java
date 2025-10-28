package io.github.thirty30ww.utils;

import io.github.thirty30ww.processor.Permit;

/**
 * 模块访问权限工具类
 * 负责在运行时动态添加对 JDK 内部模块的访问权限（Lombok 风格）
 */
public class ModuleAccessor {
    
    /**
     * 在运行时动态地为注解处理器添加对 jdk.compiler 模块的访问权限。
     * 这样使用者就不需要在 pom.xml 中配置 --add-opens 参数了。
     * 
     * 该方法适用于 JDK 9+，在 JDK 8 及以下会自动跳过。
     */
    public static void openJdkCompiler() {
        // 检查是否运行在 JDK 9+
        Class<?> cModule;
        try {
            cModule = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            // JDK 8 或更低版本，不需要处理模块访问
            return;
        }
        
        try {
            // 获取 Unsafe 实例
            sun.misc.Unsafe unsafe = Permit.getUnsafe();
            if (unsafe == null) {
                return;
            }
            
            // 获取 jdk.compiler 模块和当前模块
            Object jdkCompilerModule = getJdkCompilerModule();
            Object ownModule = getOwnModule();
            
            if (jdkCompilerModule == null || ownModule == null) {
                return;
            }
            
            // 需要开放的包列表
            String[] packagesToOpen = {
                "com.sun.tools.javac.api",
                "com.sun.tools.javac.code",
                "com.sun.tools.javac.comp",
                "com.sun.tools.javac.file",
                "com.sun.tools.javac.main",
                "com.sun.tools.javac.model",
                "com.sun.tools.javac.parser",
                "com.sun.tools.javac.processing",
                "com.sun.tools.javac.tree",
                "com.sun.tools.javac.util",
                "com.sun.tools.javac.jvm",
            };
            
            // 使用反射调用 Module.implAddOpens() 方法
            java.lang.reflect.Method implAddOpens = cModule.getDeclaredMethod(
                "implAddOpens", String.class, cModule);
            
            // 使用 Unsafe 设置方法为可访问
            long firstFieldOffset = Permit.getFirstFieldOffset(unsafe);
            unsafe.putBooleanVolatile(implAddOpens, firstFieldOffset, true);
            
            // 为每个包调用 implAddOpens
            for (String packageName : packagesToOpen) {
                implAddOpens.invoke(jdkCompilerModule, packageName, ownModule);
            }
            
        } catch (Exception e) {
            // 静默失败，如果出错，用户可能需要手动配置 --add-opens
        }
    }
    
    /**
     * 获取当前模块（注解处理器所在的模块）
     */
    private static Object getOwnModule() {
        try {
            java.lang.reflect.Method getModule = Class.class.getDeclaredMethod("getModule");
            return getModule.invoke(ModuleAccessor.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取 jdk.compiler 模块
     */
    private static Object getJdkCompilerModule() {
        try {
            // 使用反射调用: ModuleLayer.boot().findModule("jdk.compiler").get()
            Class<?> cModuleLayer = Class.forName("java.lang.ModuleLayer");
            java.lang.reflect.Method mBoot = cModuleLayer.getDeclaredMethod("boot");
            Object bootLayer = mBoot.invoke(null);
            
            java.lang.reflect.Method mFindModule = cModuleLayer.getDeclaredMethod(
                "findModule", String.class);
            Object optionalModule = mFindModule.invoke(bootLayer, "jdk.compiler");
            
            Class<?> cOptional = Class.forName("java.util.Optional");
            java.lang.reflect.Method mGet = cOptional.getDeclaredMethod("get");
            return mGet.invoke(optionalModule);
        } catch (Exception e) {
            return null;
        }
    }
}

