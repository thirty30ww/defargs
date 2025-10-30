package io.github.thirty30ww.defargs.utils;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.VariableElement;
import java.util.Map;

/**
 * 方法生成工具类
 * <p>
 * 负责生成重载方法的 AST 节点，包括参数列表、方法体（调用原方法并填充默认值）和返回语句。
 * <p>
 * 示例：
 * <pre>
 * {@code
 * // 原始方法
 * void setConfig(String host, @DefaultValue("8080") int port) { }
 * 
 * // 生成重载
 * void setConfig(String host) { setConfig(host, 8080); }
 * }
 * </pre>
 */
public class MethodGenerator {
    
    /**
     * TreeMaker：用于创建 AST 节点的工厂类
     */
    private final TreeMaker treeMaker;
    
    /**
     * Names：用于创建标识符的工厂类
     */
    private final Names names;
    
    /**
     * 构造函数：初始化方法生成器
     * 
     * @param treeMaker 用于创建 AST 节点
     * @param names 用于创建标识符
     */
    public MethodGenerator(TreeMaker treeMaker, Names names) {
        this.treeMaker = treeMaker;
        this.names = names;
    }
    
    /**
     * 创建重载方法的 AST 节点（主入口方法）
     * <p>
     * 完整示例：
     * <pre>
     * {@code
     * // 输入：
     * // originalMethod: public int calc(int a, @DefaultValue("10") int b, @DefaultValue("20") int c)
     * // parameters: [a, b, c]
     * // defaultValueMap: {1 -> "10", 2 -> "20"}
     * // dropCount: 2 （省略最后 2 个参数）
     * 
     * // 生成：
     * public int calc(int a) {
     *     return calc(a, 10, 20);
     * }
     * }
     * </pre>
     *
     * @param originalMethod 原始方法的 AST 节点
     * @param parameters 原始方法的参数列表（用于获取参数信息）
     * @param defaultValueMap 参数索引到默认值的映射（如 {1 -> "10", 2 -> "20"}）
     * @param dropCount 要省略的参数数量（从末尾开始，如 dropCount=2 表示省略最后 2 个参数）
     * @return 新生成的重载方法 AST 节点
     */
    public JCTree.JCMethodDecl createOverloadMethod(JCTree.JCMethodDecl originalMethod,
                                                   java.util.List<? extends VariableElement> parameters,
                                                   Map<Integer, String> defaultValueMap,
                                                   int dropCount) {
        // 第一步：复制方法修饰符（public, static, final 等）
        long modifiers = originalMethod.mods.flags;
        
        // 第二步：创建新的参数列表（省略末尾 dropCount 个参数）
        ListBuffer<JCTree.JCVariableDecl> newParams = createNewParameters(originalMethod, parameters, dropCount);
        
        // 第三步：创建方法体（调用原方法，补充默认值）
        JCTree.JCBlock body = createMethodBody(originalMethod, parameters, defaultValueMap, dropCount);
        
        // 第四步：组装成完整的方法声明
        return treeMaker.MethodDef(
                treeMaker.Modifiers(modifiers),    // 修饰符
                originalMethod.name,                // 方法名（与原方法相同）
                originalMethod.restype,             // 返回类型
                originalMethod.typarams,            // 泛型参数
                newParams.toList(),                 // 新的参数列表
                originalMethod.thrown,              // 异常声明
                body,                               // 方法体
                null                                // 默认值（方法没有默认值）
        );
    }
    
    /**
     * 创建新的参数列表（省略末尾的参数）
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 原始方法参数：(String name, int age, boolean active)
     * // dropCount = 2
     * 
     * createNewParameters(...)
     * -> keepCount = 3 - 2 = 1
     * -> 只保留第一个参数 "String name"
     * -> 返回：[String name]
     * 
     * // 生成的方法签名：
     * void foo(String name) { ... }
     * }
     * </pre>
     *
     * @param originalMethod 原始方法节点（用于获取参数定义）
     * @param parameters 原始方法的参数列表（用于计算保留数量）
     * @param dropCount 要省略的参数数量（从末尾开始）
     * @return 新的参数列表（只包含前 N-dropCount 个参数）
     */
    private ListBuffer<JCTree.JCVariableDecl> createNewParameters(JCTree.JCMethodDecl originalMethod,
                                                                 java.util.List<? extends VariableElement> parameters,
                                                                 int dropCount) {
        ListBuffer<JCTree.JCVariableDecl> newParams = new ListBuffer<>();
        int keepCount = parameters.size() - dropCount;  // 计算要保留的参数数量
        
        // 为新方法创建参数的副本（只保留前 keepCount 个）
        for (int i = 0; i < keepCount; i++) {
            JCTree.JCVariableDecl originalParam = originalMethod.params.get(i);
            
            // 创建参数声明（复制类型和名称，不复制默认值）
            JCTree.JCVariableDecl newParam = treeMaker.VarDef(
                    treeMaker.Modifiers(originalParam.mods.flags),  // 复制修饰符（final 等）
                    originalParam.name,                              // 复制参数名
                    originalParam.vartype,                           // 复制参数类型
                    null  // 方法参数没有初始化表达式
            );
            newParams.append(newParam);
        }
        
        return newParams;
    }
    
    /**
     * 创建方法体（调用原方法并填充默认值）
     * <p>
     * 生成的方法体包含一个语句：调用原方法并传入所有参数
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 原始方法：save(String name, int age, boolean active)
     * // dropCount = 2（省略 age 和 active）
     * // defaultValueMap: {1 -> "18", 2 -> "true"}
     * 
     * // 生成的方法体：
     * {
     *     return save(name, 18, true);
     *     // ↑         ↑    ↑   ↑
     *     // 调用原方法 保留  默认值
     * }
     * }
     * </pre>
     *
     * @param originalMethod 原始方法节点（用于获取方法名和返回类型）
     * @param parameters 原始方法的参数列表
     * @param defaultValueMap 参数索引到默认值的映射
     * @param dropCount 要省略的参数数量（从末尾开始）
     * @return 新的方法体 AST 节点
     */
    private JCTree.JCBlock createMethodBody(JCTree.JCMethodDecl originalMethod,
                                           java.util.List<? extends VariableElement> parameters,
                                           Map<Integer, String> defaultValueMap,
                                           int dropCount) {
        // 第一步：创建方法调用参数（保留的参数 + 默认值）
        ListBuffer<JCTree.JCExpression> args = createMethodCallArgs(parameters, defaultValueMap, dropCount);
        
        // 第二步：创建方法调用表达式（如：save(name, 18, true)）
        JCTree.JCExpression methodCall = treeMaker.Apply(
                List.nil(),                          // 泛型参数（通常为空）
                treeMaker.Ident(originalMethod.name), // 方法名
                args.toList()                         // 参数列表
        );
        
        // 第三步：创建语句（return 语句或普通调用语句）
        JCTree.JCStatement statement = createReturnStatement(originalMethod, methodCall);
        
        // 第四步：创建方法体代码块
        return treeMaker.Block(0, List.of(statement));
    }
    
    /**
     * 创建方法调用参数列表（保留的参数 + 默认值字面量）
     * <p>
     * 参数列表构成：[保留的参数变量引用] + [默认值字面量]
     * <p>
     * 详细示例：
     * <pre>
     * {@code
     * // 原始方法：query(String sql, int pageSize, int pageNum)
     * // parameters: [sql, pageSize, pageNum]
     * // defaultValueMap: {1 -> "10", 2 -> "1"}
     * // dropCount: 2（省略 pageSize 和 pageNum）
     * 
     * createMethodCallArgs(...)
     * 
     * // 第一阶段：添加保留的参数（keepCount = 1）
     * i=0: paramName="sql" -> treeMaker.Ident("sql")
     * args = [sql]  // 变量引用
     * 
     * // 第二阶段：添加默认值（i从1到2）
     * i=1: defaultValue="10", paramType="int"
     *   -> TypeConverter.createDefaultValueExpression(tm, "10", "int")
     *   -> treeMaker.Literal(10)
     * args = [sql, 10]  // 10 是字面量
     * 
     * i=2: defaultValue="1", paramType="int"
     *   -> TypeConverter.createDefaultValueExpression(tm, "1", "int")
     *   -> treeMaker.Literal(1)
     * args = [sql, 10, 1]  // 完整参数列表
     * 
     * // 生成的调用：query(sql, 10, 1)
     * //              ↑    ↑   ↑
     * //            变量  字面量
     * }
     * </pre>
     *
     * @param parameters 原始方法的参数列表
     * @param defaultValueMap 参数索引到默认值的映射
     * @param dropCount 要省略的参数数量（从末尾开始）
     * @return 方法调用参数列表（包含变量引用和字面量）
     */
    private ListBuffer<JCTree.JCExpression> createMethodCallArgs(java.util.List<? extends VariableElement> parameters,
                                                              Map<Integer, String> defaultValueMap,
                                                              int dropCount) {
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        int keepCount = parameters.size() - dropCount;
        
        // 第一阶段：添加保留的参数（作为变量引用）
        for (int i = 0; i < keepCount; i++) {
            String paramName = parameters.get(i).getSimpleName().toString();
            // 创建变量引用（如：sql, name, id）
            args.append(treeMaker.Ident(names.fromString(paramName)));
        }
        
        // 第二阶段：添加默认值参数（作为字面量）
        for (int i = keepCount; i < parameters.size(); i++) {
            String defaultValue = defaultValueMap.get(i);
            String paramType = parameters.get(i).asType().toString();
            
            // 创建默认值字面量（如：10, "hello", true）
            JCTree.JCExpression defaultExpr = TypeConverter.createDefaultValueExpression(treeMaker, defaultValue, paramType);
            args.append(defaultExpr);
        }
        
        return args;
    }
    
    /**
     * 创建返回语句（根据返回类型决定是否需要 return 关键字）
     * <p>
     * 两种情况：
     * <ol>
     *   <li>void 方法：直接调用，不需要 return</li>
     *   <li>非 void 方法：需要 return 返回值</li>
     * </ol>
     * <p>
     * 示例：
     * <pre>
     * {@code
     * // 情况 1：void 方法
     * // 原始方法：void save(String name, int age)
     * // 生成：save(name, 18);  // 不带 return
     * 
     * // 情况 2：有返回值的方法
     * // 原始方法：int calc(int a, int b)
     * // 生成：return calc(a, 10);  // 带 return
     * }
     * </pre>
     *
     * @param originalMethod 原始方法节点（用于获取返回类型）
     * @param methodCall 方法调用表达式（如：save(name, 18)）
     * @return 返回语句节点（Exec 或 Return）
     */
    private JCTree.JCStatement createReturnStatement(JCTree.JCMethodDecl originalMethod, JCTree.JCExpression methodCall) {
        // 检查返回类型
        if (originalMethod.restype.type.toString().equals("void")) {
            // void 方法：创建普通执行语句（不带 return）
            // 生成：save(name, 18);
            return treeMaker.Exec(methodCall);
        } else {
            // 非 void 方法：创建 return 语句
            // 生成：return calc(a, 10);
            return treeMaker.Return(methodCall);
        }
    }
}

