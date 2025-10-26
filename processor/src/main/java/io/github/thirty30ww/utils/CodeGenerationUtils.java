package io.github.thirty30ww.utils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.*;

/**
 * 代码生成工具类
 * 提供各种代码生成相关的实用方法
 */
public class CodeGenerationUtils {

    /**
     * 提取末尾连续的默认参数索引
     * 
     * @param defaultIdxs    所有带默认值的参数索引列表
     * @param parametersSize 参数总数
     * @return 末尾连续的默认参数索引列表，按从小到大排序
     */
    public static List<Integer> extractTrailingDefaultParameters(List<Integer> defaultIdxs, int parametersSize) {
        List<Integer> trailing = new ArrayList<>();
        
        // 如果没有默认参数，直接返回空列表
        if (defaultIdxs.isEmpty()) {
            return trailing;
        }
        
        // 创建副本以避免修改原列表
        List<Integer> sortedIdxs = new ArrayList<>(defaultIdxs);
        Collections.sort(sortedIdxs);
        
        // 从参数列表末尾开始，向前查找连续的默认参数
        int last = parametersSize - 1;
        while (!sortedIdxs.isEmpty() && sortedIdxs.get(sortedIdxs.size() - 1) == last) {
            trailing.add(last);
            sortedIdxs.remove(sortedIdxs.size() - 1);
            last--;
        }
        
        // 将结果反转，使其按从小到大的顺序排列
        Collections.reverse(trailing);
        
        return trailing;
    }

    /**
     * 生成重载方法的签名部分
     *
     * @param method     原始方法
     * @param parameters 方法参数列表
     * @param drop       要省略的末尾参数个数
     * @return 生成的方法签名字符串
     */
    public static String generateMethodSignature(ExecutableElement method, 
                                               List<? extends VariableElement> parameters, 
                                               int drop) {
        String methodName = method.getSimpleName().toString();
        StringBuilder signature = new StringBuilder();
        
        signature.append("    public ")
                 .append(method.getReturnType())
                 .append(" ")
                 .append(methodName)
                 .append("(");
        
        List<String> sigParams = new ArrayList<>();
        for (int i = 0; i < parameters.size() - drop; i++) {
            VariableElement p = parameters.get(i);
            sigParams.add(p.asType().toString() + " " + p.getSimpleName());
        }
        
        signature.append(String.join(", ", sigParams));
        signature.append(") {");
        
        return signature.toString();
    }
    
    /**
     * 生成重载方法的方法体部分
     *
     * @param method          原始方法
     * @param parameters      方法参数列表
     * @param defaultValueMap 默认值映射表
     * @param drop            要省略的末尾参数个数
     * @return 生成的方法体字符串
     */
    public static String generateMethodBody(ExecutableElement method,
                                          List<? extends VariableElement> parameters,
                                          Map<Integer, String> defaultValueMap,
                                          int drop) {
        String methodName = method.getSimpleName().toString();
        StringBuilder body = new StringBuilder();
        
        body.append("        return super.")
            .append(methodName)
            .append("(");
        
        List<String> callParams = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            if (i < parameters.size() - drop) {
                // 使用传入的参数
                callParams.add(parameters.get(i).getSimpleName().toString());
            } else {
                // 使用默认值
                String raw = defaultValueMap.get(i);
                String lit = TypeConversionUtils.toSourceLiteral(parameters.get(i).asType().toString(), raw);
                callParams.add(lit);
            }
        }
        
        body.append(String.join(", ", callParams));
        body.append(");");
        
        return body.toString();
    }


     /**
      * 生成包声明
      *
      * @param packageName 包名
      * @return 包声明字符串
      */
     public static String generatePackageDeclaration(String packageName) {
         if (packageName == null || packageName.trim().isEmpty()) {
             return "";
         }
         return "package " + packageName + ";";
     }

     /**
      * 生成类声明
      *
      * @param className        类名
      * @param superClassName   父类名（可为null）
      * @param comment          类注释（可为null）
      * @return 类声明字符串
      */
     public static String generateClassDeclaration(String className, String superClassName, String comment) {
         StringBuilder classDecl = new StringBuilder();
         
         // 添加注释
         if (comment != null && !comment.trim().isEmpty()) {
             classDecl.append("// ").append(comment).append("\n");
         }
         
         // 添加类声明
         classDecl.append("public class ").append(className);
         
         // 添加继承关系
         if (superClassName != null && !superClassName.trim().isEmpty()) {
             classDecl.append(" extends ").append(superClassName);
         }
         
         classDecl.append(" {");
         
         return classDecl.toString();
     }

     /**
      * 生成完整的类文件头部（包声明 + 空行 + 类声明）
      *
      * @param packageName      包名
      * @param className        类名
      * @param superClassName   父类名（可为null）
      * @param comment          类注释（可为null）
      * @return 完整的类文件头部
      */
     public static String generateClassHeader(String packageName, String className, 
                                            String superClassName, String comment) {
         StringBuilder header = new StringBuilder();
         
         // 包声明
         String packageDecl = generatePackageDeclaration(packageName);
         if (!packageDecl.isEmpty()) {
             header.append(packageDecl).append("\n\n");
         }
         
         // 类声明
         header.append(generateClassDeclaration(className, superClassName, comment));
         
         return header.toString();
     }
}