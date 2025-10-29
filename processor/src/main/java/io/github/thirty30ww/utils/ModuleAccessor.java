package io.github.thirty30ww.utils;

import io.github.thirty30ww.processor.Permit;

/**
 * 模块访问权限工具类
 * <p>
 * 模块破解，在运行时通过反射 + Unsafe 动态开放 jdk.compiler 模块的访问权限，
 * 使用者无需配置 --add-opens 参数。
 * <p>
 * 兼容 JDK 8（自动跳过）和 JDK 9+（动态开放权限）。
 */
public class ModuleAccessor {
    
    /**
     * 在运行时动态地为注解处理器添加对 jdk.compiler 模块的访问权限
     * <p>
     * 使用场景：在注解处理器的 init() 方法中调用，确保后续能访问 Javac 内部 API
     * <p>
     * 示例：
     * <pre>
     * {@code
     * @Override
     * public synchronized void init(ProcessingEnvironment processingEnv) {
     *     super.init(processingEnv);
     *
     *     // 动态开放模块访问权限
     *     ModuleAccessor.openJdkCompiler();
     *
     *     // 现在可以安全地使用 Javac 内部 API
     *     Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
     *     TreeMaker treeMaker = TreeMaker.instance(context);  // ✓ 不会报错
     * }
     * }
     * </pre>
     * <p>
     * 优势：
     * <ul>
     *   <li>用户无需配置 --add-opens 参数</li>
     *   <li>跨 JDK 版本兼容（JDK 8-21+）</li>
     *   <li>开箱即用，零配置</li>
     * </ul>
     * <p>
     * 注意事项：
     * <ul>
     *   <li>该方法适用于 JDK 9+，在 JDK 8 及以下会自动跳过</li>
     *   <li>如果开放失败会静默失败（用户可能需要手动配置 --add-opens）</li>
     * </ul>
     */
    public static void openJdkCompiler() {
        // 第一步：检查是否运行在 JDK 9+
        // 尝试加载 java.lang.Module 类（JDK 9+ 才有）
        Class<?> cModule;
        try {
            cModule = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            // JDK 8 或更低版本，没有模块系统，不需要处理
            return;
        }
        
        try {
            // 第二步：获取 Unsafe 实例（绕过 Java 安全检查的神器）
            sun.misc.Unsafe unsafe = Permit.getUnsafe();
            if (unsafe == null) {
                return;  // 无法获取 Unsafe，放弃
            }
            
            // 第三步：获取 jdk.compiler 模块和当前模块
            Object jdkCompilerModule = getJdkCompilerModule();  // jdk.compiler 模块
            Object ownModule = getOwnModule();                   // 我们自己的模块
            
            if (jdkCompilerModule == null || ownModule == null) {
                return;  // 无法获取模块，放弃
            }
            
            // 第四步：定义需要开放的包列表
            // 这些都是 Javac 内部 API 的包，我们需要访问它们
            String[] packagesToOpen = {
                "com.sun.tools.javac.api",        // Javac API
                "com.sun.tools.javac.code",       // 符号和类型系统
                "com.sun.tools.javac.comp",       // 编译器组件
                "com.sun.tools.javac.file",       // 文件管理
                "com.sun.tools.javac.main",       // 主程序
                "com.sun.tools.javac.model",      // 语言模型
                "com.sun.tools.javac.parser",     // 解析器
                "com.sun.tools.javac.processing", // 注解处理
                "com.sun.tools.javac.tree",       // AST 树（最重要！）
                "com.sun.tools.javac.util",       // 工具类（TreeMaker, Names 等）
                "com.sun.tools.javac.jvm",        // JVM 字节码生成
            };
            
            // 第五步：使用反射获取 Module.implAddOpens() 方法
            // 这是 JDK 内部方法，用于开放模块访问权限
            java.lang.reflect.Method implAddOpens = cModule.getDeclaredMethod(
                "implAddOpens", String.class, cModule);
            
            // 第六步：使用 Unsafe 强制设置方法为可访问
            // implAddOpens 是 private 方法，需要通过 Unsafe 绕过访问检查
            long firstFieldOffset = Permit.getFirstFieldOffset(unsafe);
            unsafe.putBooleanVolatile(implAddOpens, firstFieldOffset, true);
            
            // 第七步：为每个包调用 implAddOpens，开放访问权限
            for (String packageName : packagesToOpen) {
                // 相当于：jdkCompilerModule.implAddOpens(packageName, ownModule)
                // 效果：允许 ownModule 访问 jdkCompilerModule 中的 packageName 包
                implAddOpens.invoke(jdkCompilerModule, packageName, ownModule);
            }
            
        } catch (Exception e) {
            // 静默失败：如果出错（比如新版本 JDK 修改了内部实现），
            // 用户可能需要手动配置 --add-opens 参数
        }
    }
    
    /**
     * 获取当前模块（注解处理器所在的模块）
     * <p>
     * 实现原理：通过反射调用 Class.getModule() 方法
     * <p>
     * 等价代码：
     * <pre>
     * {@code
     * Module ownModule = ModuleAccessor.class.getModule();
     * }
     * </pre>
     * 
     * @return 当前模块对象，如果获取失败则返回 null
     */
    private static Object getOwnModule() {
        try {
            // 反射获取 Class.getModule() 方法（JDK 9+ 新增）
            java.lang.reflect.Method getModule = Class.class.getDeclaredMethod("getModule");
            
            // 调用 ModuleAccessor.class.getModule()
            return getModule.invoke(ModuleAccessor.class);
        } catch (Exception e) {
            return null;  // 获取失败（可能是 JDK 8）
        }
    }
    
    /**
     * 获取 jdk.compiler 模块
     * <p>
     * 实现原理：通过反射调用 ModuleLayer API
     * <p>
     * 等价代码：
     * <pre>
     * {@code
     * ModuleLayer bootLayer = ModuleLayer.boot();
     * Optional<Module> optionalModule = bootLayer.findModule("jdk.compiler");
     * Module jdkCompilerModule = optionalModule.get();
     * }
     * </pre>
     * <p>
     * 执行步骤：
     * <ol>
     *   <li>获取启动层（boot layer）：包含所有系统模块</li>
     *   <li>在启动层中查找 "jdk.compiler" 模块</li>
     *   <li>从 Optional 中提取模块对象</li>
     * </ol>
     * 
     * @return jdk.compiler 模块对象，如果获取失败则返回 null
     */
    private static Object getJdkCompilerModule() {
        try {
            // 第一步：获取 ModuleLayer 类（JDK 9+ 的模块层 API）
            Class<?> cModuleLayer = Class.forName("java.lang.ModuleLayer");
            
            // 第二步：调用 ModuleLayer.boot() 获取启动层
            java.lang.reflect.Method mBoot = cModuleLayer.getDeclaredMethod("boot");
            Object bootLayer = mBoot.invoke(null);  // 静态方法，传 null
            
            // 第三步：调用 bootLayer.findModule("jdk.compiler")
            java.lang.reflect.Method mFindModule = cModuleLayer.getDeclaredMethod(
                "findModule", String.class);
            Object optionalModule = mFindModule.invoke(bootLayer, "jdk.compiler");
            
            // 第四步：从 Optional<Module> 中提取 Module
            Class<?> cOptional = Class.forName("java.util.Optional");
            java.lang.reflect.Method mGet = cOptional.getDeclaredMethod("get");
            return mGet.invoke(optionalModule);  // optionalModule.get()
        } catch (Exception e) {
            return null;  // 获取失败（可能找不到 jdk.compiler 模块）
        }
    }
}

