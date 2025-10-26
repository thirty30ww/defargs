package io.github.thirty30ww.processor;

import io.github.thirty30ww.annotation.DefaultValue;
import io.github.thirty30ww.utils.CodeGenerationUtils;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * 一个注解处理器，用于处理 {@link DefaultValue} 注解。
 * 它会扫描方法参数上的 {@link DefaultValue} 注解，为所在类生成一个“派生类”，在该类中补充省略默认参数的重载方法。
 */
@AutoService(Processor.class)   // 自动注册这个类为注解处理器，编译时会被自动发现
@SupportedAnnotationTypes("io.github.thirty30ww.annotation.DefaultValue")   // 声明这个处理器支持处理的注解类型，这里是 DefaultValue 注解
@SupportedSourceVersion(SourceVersion.RELEASE_17)   // 声明这个处理器支持的 Java 版本，这里是 Java 17
public class DefaultValueProcessor extends AbstractProcessor {  // 继承 AbstractProcessor 类，这是所有注解处理器的基类

    private Messager messager;  // 用于在编译期间输出消息（错误、警告、信息等）
    private Filer filer;    // 用于创建新的源文件或资源文件

    /**
     * 初始化注解处理器，获取必要的工具和环境。
     *
     * @param processingEnv 提供了与编译器交互的环境，如消息打印、文件操作等。
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();    // 用来打印编译期消息
        this.filer = processingEnv.getFiler();          // 用来创建新的源文件
    }

    /**
     * 处理注解，生成新的源文件。
     *
     * @param annotations 被支持的注解类型集合。
     * @param roundEnv    提供了当前和之前的注解处理环境，用于查询被注解的元素。
     * @return 如果处理成功且后续处理器不应继续处理，则返回 true；否则返回 false。
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 遍历所有支持的注解类型（这里只有一个：DefaultValue）
        for (TypeElement annotation : annotations) {
            // 获取所有被 @DefaultValue 标注的元素
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);

            // 按类分组处理（使用 Set 去重：同一方法若有多个参数带注解，避免重复）
            Map<TypeElement, Set<ExecutableElement>> classMethods = new HashMap<>();

            // 遍历这些元素，筛选出参数元素
            for (Element element : elements) {
                // 只处理参数上的注解，忽略其他位置的注解
                if (element.getKind() == ElementKind.PARAMETER) {
                    // 找到参数所在的方法和类
                    Element enclosingElement = element.getEnclosingElement();   // 获取参数所在的方法元素
                    if (enclosingElement instanceof ExecutableElement method) { // 确保是方法元素
                        TypeElement classElement = (TypeElement) method.getEnclosingElement();   // 获取方法所在的类元素

                        // 按类分组存储方法
                        // 确保每个类的方法集合是 LinkedHashSet，保持插入顺序
                        classMethods.computeIfAbsent(classElement, k -> new LinkedHashSet<>())
                                .add(method);
                    }
                }
            }
            // 遍历每个类及其方法集合，为每个类生成重载方法
            for (Map.Entry<TypeElement, Set<ExecutableElement>> entry : classMethods.entrySet()) {
                try {
                    // 为当前类生成重载方法集合
                    generateOverloadedMethods(entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Failed to generate code: " + e.getMessage());
                }
            }
        }
        return true;
    }

    /**
     * 为给定类生成一个派生类，并在其中为包含 @DefaultValue 参数的方法生成“省略默认参数”的重载。
     *
     * @param classElement 包含默认参数方法的类。
     * @param methods      该类中所有包含默认参数的方法。
     */
    private void generateOverloadedMethods(TypeElement classElement,
                                           Collection<ExecutableElement> methods) throws IOException {
        // 提取类的包名和类名
        String packageName = getPackageName(classElement);
        String className = classElement.getSimpleName().toString();
        String generatedClassName = className + "Overloaded";

        // 使用 filer 创建新的 Java 源文件
        JavaFileObject builderFile = filer.createSourceFile(
                packageName + "." + generatedClassName);

        // 写入新源文件的内容
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            // 生成类文件头部（包声明 + 类声明）
            String classHeader = CodeGenerationUtils.generateClassHeader(
                packageName, 
                generatedClassName, 
                className, 
                "自动生成的重载方法类：继承原类，便于直接调用原始实现"
            );
            out.println(classHeader);
            out.println();

            // 为每个方法生成重载版本
            for (ExecutableElement method : methods) {
                generateOverloadedMethod(out, method, classElement);
            }

            out.println("}");
        }
    }

    /**
     * 为单个方法生成“省略带默认值参数”的重载版本。
     * <p>
     * 例如：<code>threeArgs(String a, @DefaultValue("B") String b, @DefaultValue("C") String c)</code>
     * <p>
     * 会生成：<code>threeArgs(String a, String b) => threeArgs(a, b, "C")</code>
     * <p>
     * 以及：<code>threeArgs(String a) => threeArgs(a, "B", "C")</code>
     * <p>
     * 注意：如果方法有多个连续的默认参数，会生成多个重载版本。只支持省略末尾的默认参数。
     * @param out          用于写入新源文件内容的打印写入器。
     * @param method       包含默认参数的方法。
     * @param classElement 该方法所属的类。
     */
    private void generateOverloadedMethod(PrintWriter out, ExecutableElement method,
                                          TypeElement classElement) {
        // 提取方法名和参数列表
        String methodName = method.getSimpleName().toString();
        List<? extends VariableElement> parameters = method.getParameters();

        // 记录这些参数的索引位置和默认值
        List<Integer> defaultIdxs = new ArrayList<>();
        Map<Integer, String> defaultValueMap = new HashMap<>();
        // 遍历所有参数，找出带有 @DefaultValue 注解的参数
        for (int i = 0; i < parameters.size(); i++) {
            // 检查当前参数是否带有 @DefaultValue 注解
            VariableElement param = parameters.get(i);
            DefaultValue annotation = param.getAnnotation(DefaultValue.class);

            // 如果有注解，记录参数索引和默认值
            if (annotation != null) {
                defaultIdxs.add(i);
                defaultValueMap.put(i, annotation.value());
            }
        }

        // 如果没有找到带注解的参数，直接返回
        if (defaultIdxs.isEmpty()) {
            return; // 没有找到带注解的参数
        }

        // 提取末尾连续的默认参数
        List<Integer> trailing = CodeGenerationUtils.extractTrailingDefaultParameters(defaultIdxs, parameters.size());
        
        if (trailing.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "Only trailing @DefaultValue parameters are supported for now", method);
            return;
        }

        // 生成 m 条重载（省略末尾 1..m 个默认参数）
        for (int drop = 1; drop <= trailing.size(); drop++) {
            out.println("    // 自动生成的重载方法（省略末尾 " + drop + " 个默认参数）");
            
            // 生成方法签名
            String methodSignature = CodeGenerationUtils.generateMethodSignature(method, parameters, drop);
            out.println(methodSignature);
            
            // 生成方法体
            String methodBody = CodeGenerationUtils.generateMethodBody(method, parameters, defaultValueMap, drop);
            out.println(methodBody);
            
            out.println("    }");
            out.println();
        }
    }


    /** 获取类的包名 */
    private String getPackageName(TypeElement classElement) {
        return processingEnv.getElementUtils().getPackageOf(classElement).toString();
    }

}