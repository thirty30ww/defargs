package io.github.thirty30ww.utils;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * AST 操作工具类
 * <p>
 * 负责 AST 节点的查找、匹配和类型名称简化等操作。
 */
public class ASTOperator {
    
    /**
     * TreeMaker：用于创建 AST 节点的工厂类
     * <p>
     * 作用：生成各种 AST 节点，如方法声明、变量声明、表达式等
     */
    private final TreeMaker treeMaker;
    
    /**
     * Names：用于创建标识符（变量名、方法名等）的工厂类
     * <p>
     * 作用：将字符串转换为 Javac 内部使用的 Name 对象
     */
    private final Names names;
    
    /**
     * 构造函数：从 Javac 上下文中初始化 TreeMaker 和 Names 实例
     * 
     * @param context Javac 编译上下文，包含编译器的所有状态信息
     */
    public ASTOperator(Context context) {
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }
    
    /**
     * 在类的 AST 中查找对应的方法节点
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 原始类：
     * class User {
     *     void save(String name, int age) { }
     *     void save(String name) { }  // 重载方法
     * }
     * 
     * // 查找 save(String, int) 方法
     * ExecutableElement method = ...; // save(String, int)
     * JCTree.JCMethodDecl methodTree = findMethodTree(classTree, method);
     * // 返回：save(String name, int age) 的 AST 节点
     * }
     * </pre>
     *
     * @param classTree 类的 AST 节点
     * @param method 方法元素（编译器抽象）
     * @return 对应的方法 AST 节点，如果不存在则返回 null
     */
    public JCTree.JCMethodDecl findMethodTree(JCTree.JCClassDecl classTree, ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        java.util.List<? extends VariableElement> methodParams = method.getParameters();

        // 遍历类的所有成员定义（字段、方法、构造函数等）
        for (JCTree def : classTree.defs) {
            if (def instanceof JCTree.JCMethodDecl methodDecl) {
                // 先检查方法名和参数数量
                if (methodDecl.name.toString().equals(methodName) &&
                    methodDecl.params.size() == methodParams.size()) {

                    // 再检查参数类型是否匹配
                    if (isParameterTypesMatch(methodParams, methodDecl)) {
                        return methodDecl;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 检查参数类型是否匹配（用于 ExecutableElement 和 JCMethodDecl 之间的比较）
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 这些类型被认为是匹配的：
     * "List<String>" ↔ "List<Integer>"  // 忽略泛型参数
     * "java.util.List" ↔ "List"          // 忽略包名
     * "String" ↔ "java.lang.String"      // 忽略包名
     * 
     * // 这些类型被认为是不匹配的：
     * "String" ↔ "Integer"  // 基础类型名不同
     * "List" ↔ "Map"        // 基础类型名不同
     * }
     * </pre>
     *
     * @param methodParams 方法参数元素列表（来自 ExecutableElement）
     * @param methodDecl 方法 AST 节点
     * @return 如果所有参数类型都匹配则返回 true，否则返回 false
     */
    private boolean isParameterTypesMatch(java.util.List<? extends VariableElement> methodParams, 
                                        JCTree.JCMethodDecl methodDecl) {
        for (int i = 0; i < methodParams.size(); i++) {
            String expectedType = methodParams.get(i).asType().toString();
            String actualType = methodDecl.params.get(i).vartype.type.toString();
            
            // 简化类型名称后比较（去除包名和泛型）
            // 例如：List<String> 和 List<Integer> 被认为是匹配的
            if (!simplifyTypeName(expectedType).equals(simplifyTypeName(actualType))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查类中是否已存在指定签名的方法
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 原始方法
     * void save(String name, @DefaultValue("18") int age) { }
     * 
     * // 检查是否已存在重载方法 save(String)
     * boolean exists = methodExists(classTree, "save", originalMethod, 1);
     * 
     * // 如果类中已有：
     * void save(String name) { }  // 返回 true，避免重复生成
     * 
     * // 如果类中只有原始方法，返回 false，可以生成
     * }
     * </pre>
     * 
     * @param classTree 类的 AST 节点
     * @param methodName 方法名称
     * @param originalMethod 原始方法节点（用于获取参数类型和排除自身）
     * @param paramCount 要检查的参数数量
     * @return 如果类中已存在相同签名的方法则返回 true，否则返回 false
     */
    public boolean methodExists(JCTree.JCClassDecl classTree, String methodName,
                              JCTree.JCMethodDecl originalMethod, int paramCount) {
        // 遍历类的所有成员
        for (JCTree def : classTree.defs) {
            if (def instanceof JCTree.JCMethodDecl methodDecl) {
                // 跳过原始方法本身
                if (methodDecl == originalMethod) {
                    continue;
                }
                
                // 检查方法名和参数数量
                if (methodDecl.name.toString().equals(methodName) &&
                    methodDecl.params.size() == paramCount) {
                    
                    // 检查前 paramCount 个参数的类型是否匹配
                    if (isParameterTypesMatch(originalMethod, methodDecl, paramCount)) {
                        return true;  // 找到匹配的方法
                    }
                }
            }
        }
        return false;  // 没有找到匹配的方法
    }
    
    /**
     * 检查两个方法的参数类型是否匹配（用于两个 JCMethodDecl 之间的比较）
     * <p>
     * 匹配规则：只比较前 paramCount 个参数，忽略泛型信息和包名
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 原始方法：save(String name, int age, boolean active)
     * // 现有方法：save(String name, int age)
     * // paramCount = 2
     * 
     * isParameterTypesMatch(originalMethod, existingMethod, 2)
     * -> 比较第 0 个参数：String ↔ String ✓
     * -> 比较第 1 个参数：int ↔ int ✓
     * -> 返回 true（前 2 个参数匹配）
     * }
     * </pre>
     *
     * @param originalMethod 原始方法节点
     * @param methodDecl 要检查的方法节点
     * @param paramCount 要比较的参数数量
     * @return 如果前 paramCount 个参数类型都匹配则返回 true，否则返回 false
     */
    private boolean isParameterTypesMatch(JCTree.JCMethodDecl originalMethod, 
                                        JCTree.JCMethodDecl methodDecl, int paramCount) {
        // 只比较前 paramCount 个参数
        for (int i = 0; i < paramCount && i < originalMethod.params.size(); i++) {
            String originalType = originalMethod.params.get(i).vartype.type.toString();
            String existingType = methodDecl.params.get(i).vartype.type.toString();
            
            // 简化类型名称后比较（去除包名和泛型）
            if (!simplifyTypeName(originalType).equals(simplifyTypeName(existingType))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 简化类型名称，去除包名和泛型信息
     * <p>
     * 示例：
     * <pre>
     * {@code
     * simplifyTypeName("java.util.List<String>")     -> "List"
     * simplifyTypeName("java.lang.String")            -> "String"
     * simplifyTypeName("List<Map<String, Integer>>")  -> "List"
     * simplifyTypeName("int")                         -> "int"
     * simplifyTypeName("com.example.User")            -> "User"
     * }
     * </pre>
     *
     * @param typeName 原始类型名称（可能包含包名和泛型）
     * @return 简化后的类型名称（只保留基础类型名）
     */
    public String simplifyTypeName(String typeName) {
        // 第一步：去除泛型信息
        // "List<String>" -> "List"
        int genericIndex = typeName.indexOf('<');
        if (genericIndex > 0) {
            typeName = typeName.substring(0, genericIndex);
        }
        
        // 第二步：去除包名
        // "java.util.List" -> "List"
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot > 0) {
            typeName = typeName.substring(lastDot + 1);
        }
        
        return typeName;
    }
    
    /**
     * 获取 TreeMaker 实例
     * <p>
     * TreeMaker 用于创建各种 AST 节点，如方法声明、变量声明、表达式等
     * 
     * @return TreeMaker 实例
     */
    public TreeMaker getTreeMaker() {
        return treeMaker;
    }
    
    /**
     * 获取 Names 实例
     * <p>
     * Names 用于将字符串转换为 Javac 内部使用的 Name 对象
     * 
     * @return Names 实例
     */
    public Names getNames() {
        return names;
    }
}
