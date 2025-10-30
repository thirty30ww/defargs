package io.github.thirty30ww.defargs.processor;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import io.github.thirty30ww.defargs.annotation.DefaultValue;
import io.github.thirty30ww.defargs.utils.ASTOperator;
import io.github.thirty30ww.defargs.utils.MethodGenerator;
import io.github.thirty30ww.defargs.utils.ParameterAnalyzer;
import io.github.thirty30ww.defargs.utils.ModuleAccessor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link DefaultValue} 注解处理器
 * <p>
 * 使用 Javac Tree API 直接在原类中添加重载方法，为带 @DefaultValue 注解的方法生成省略默认参数的重载版本。
 * <p>
 * 示例：
 * <pre>
 * {@code
 * // 原始方法
 * void query(String sql, @DefaultValue("10") int pageSize) { }
 * 
 * // 自动生成
 * void query(String sql) { query(sql, 10); }
 * }
 * </pre>
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("io.github.thirty30ww.defargs.annotation.DefaultValue")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class DefaultValueProcessor extends AbstractProcessor {

    private Messager messager;
    private Trees trees;
    private ASTOperator astOperator;
    private ParameterAnalyzer parameterAnalyzer;
    private MethodGenerator methodGenerator;

    /**
     * 初始化注解处理器，获取必要的工具和环境
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);  // 初始化父类，获取必要的工具和环境

        ModuleAccessor.openJdkCompiler();     // 在初始化时动态添加模块访问权限

        // 解包 IntelliJ IDEA 包装的 ProcessingEnvironment（必须在使用前解包）
        ProcessingEnvironment unwrappedEnv = Unwrap.processingEnv(processingEnv);
        
        // 使用解包后的环境初始化工具
        this.messager = unwrappedEnv.getMessager();
        this.trees = Trees.instance(unwrappedEnv);
        
        // 获取 Javac 的内部上下文
        Context context = ((JavacProcessingEnvironment) unwrappedEnv).getContext();
        
        // 初始化工具类
        this.astOperator = new ASTOperator(context);
        this.parameterAnalyzer = new ParameterAnalyzer();
        this.methodGenerator = new MethodGenerator(astOperator.getTreeMaker(), astOperator.getNames());
        
        // 使用 Messager 在编译时输出日志，提示注解处理器已初始化
        messager.printMessage(Diagnostic.Kind.NOTE, "【DefaultValue】注解处理器已初始化");
    }

    /**
     * 处理注解，直接在原类的 AST 中添加重载方法
     *
     * @param annotations 被支持的注解类型集合
     * @param roundEnv 注解处理环境
     * @return true 表示注解已被处理
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 检查是否是最后一轮处理，如果是则返回 false，后续处理器不应继续处理
        if (roundEnv.processingOver()) {
            return false;
        }

        // 按类分组处理
        Map<TypeElement, Set<ExecutableElement>> classMethods = groupMethodsByClass(roundEnv);

        // 为每个类添加重载方法
        for (Map.Entry<TypeElement, Set<ExecutableElement>> entry : classMethods.entrySet()) {
            processClass(entry.getKey(), entry.getValue());
        }

        return true;
    }

    /**
     * 按类分组，收集每个类中带 @DefaultValue 注解的方法
     * <p>
     * 示例：
     * <pre>
     * {@code
     * class UserService {
     *     void save(@DefaultValue("admin") String name) { }
     * }
     * class OrderService {
     *     void create(@DefaultValue("1") int count) { }
     * }
     * // 返回：{ UserService -> [save], OrderService -> [create] }
     * }
     * </pre>
     *
     * @param roundEnv 注解处理环境
     * @return 类元素到方法集合的映射
     */
    private Map<TypeElement, Set<ExecutableElement>> groupMethodsByClass(RoundEnvironment roundEnv) {
            Map<TypeElement, Set<ExecutableElement>> classMethods = new HashMap<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(DefaultValue.class)) {
                if (element.getKind() == ElementKind.PARAMETER) {
                Element enclosingElement = element.getEnclosingElement();
                if (enclosingElement instanceof ExecutableElement method) {
                    TypeElement classElement = (TypeElement) method.getEnclosingElement();
                        classMethods.computeIfAbsent(classElement, k -> new LinkedHashSet<>())
                                .add(method);
                    }
                }
            }

        return classMethods;
    }

    /**
     * 处理单个类，为其中的方法添加重载版本
     * <p>
     * 将生成的重载方法直接添加到类的 AST 中。
     *
     * @param classElement 要处理的类元素
     * @param methods 该类中所有包含 @DefaultValue 注解的方法
     */
    private void processClass(TypeElement classElement, Set<ExecutableElement> methods) {
        try {
            // 获取类的语法树
            var treePath = trees.getPath(classElement);
            var compilationUnit = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();
            var classTree = (JCTree.JCClassDecl) treePath.getLeaf();
            
            // 设置 TreeMaker 的位置，避免 IllegalArgumentException
            astOperator.getTreeMaker().pos = classTree.pos;
            
            // 收集需要生成的重载方法
            ListBuffer<JCTree.JCMethodDecl> newMethods = new ListBuffer<>();

            // 为每个方法生成重载版本
            for (ExecutableElement method : methods) {
                generateOverloadsForMethod(method, classTree, newMethods);
            }
            
            // 将新方法添加到类定义中
            if (!newMethods.isEmpty()) {
                // 将新方法逐个添加到类的成员列表中
                for (JCTree.JCMethodDecl method : newMethods) {
                    classTree.defs = classTree.defs.append(method);
                }
                messager.printMessage(Diagnostic.Kind.NOTE,
                        "【DefaultValue】为类 " + classElement.getSimpleName() +
                        " 生成了 " + newMethods.size() + " 个重载方法");
            }
            
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "处理类 " + classElement.getSimpleName() + " 时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 为单个方法生成重载版本
     * <p>
     * 分析参数并生成所有可能的重载组合。
     *
     * @param method 要生成重载方法的原始方法
     * @param classTree 类的 AST 节点
     * @param newMethods 用于存储新生成的重载方法的列表
     */
    private void generateOverloadsForMethod(ExecutableElement method, 
                                           JCTree.JCClassDecl classTree,
                                           ListBuffer<JCTree.JCMethodDecl> newMethods) {
        // 获取方法的 AST 节点
        JCTree.JCMethodDecl methodTree = astOperator.findMethodTree(classTree, method);
        if (methodTree == null) {
            messager.printMessage(Diagnostic.Kind.WARNING, 
                    "找不到方法: " + method.getSimpleName(), method);
            return;
        }
        
        // 分析参数
        ParameterAnalyzer.Result analysisResult;
        try {
            analysisResult = parameterAnalyzer.analyzeParameters(method);
        } catch (IllegalArgumentException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage(), method);
            return;
        }

        // 检查是否有默认参数
        if (!analysisResult.hasDefaults()) {
            return;
        }

        // 检查是否有末尾连续的默认参数
        if (!analysisResult.hasTrailingDefaults()) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "目前只支持末尾连续的 @DefaultValue 参数", method);
            return;
        }

        // 为每个可能的省略组合生成重载方法
        createOverloadVariants(method, methodTree, classTree, analysisResult, newMethods);
    }
    
    /**
     * 为每个可能的省略组合生成重载方法
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 原始方法：foo(String a, @DefaultValue("1") int b, @DefaultValue("2") int c)
     * // trailingDefaults = [1, 2]
     * 
     * // 生成两个重载：
     * drop=1: foo(String a, int b) { foo(a, b, 2); }
     * drop=2: foo(String a) { foo(a, 1, 2); }
     * }
     * </pre>
     *
     * @param method 原始方法元素
     * @param methodTree 原始方法的 AST 节点
     * @param classTree 类的 AST 节点
     * @param analysisResult 参数分析结果
     * @param newMethods 用于存储新生成的重载方法的列表
     */
    private void createOverloadVariants(ExecutableElement method,
                                       JCTree.JCMethodDecl methodTree,
                                       JCTree.JCClassDecl classTree,
                                       ParameterAnalyzer.Result analysisResult,
                                       ListBuffer<JCTree.JCMethodDecl> newMethods) {
        java.util.List<Integer> trailingDefaults = analysisResult.trailingDefaults();
        
        for (int drop = 1; drop <= trailingDefaults.size(); drop++) {
            // 检查是否已存在相同签名的方法
            int newParamCount = method.getParameters().size() - drop;
            if (astOperator.methodExists(classTree, method.getSimpleName().toString(),
                           methodTree, newParamCount)) {
                continue;  // 静默跳过已存在的方法
            }
            
            // 创建新的重载方法
            JCTree.JCMethodDecl overloadMethod = methodGenerator.createOverloadMethod(
                    methodTree, method.getParameters(), analysisResult.defaultValueMap(), drop);
            newMethods.append(overloadMethod);
        }
    }
}