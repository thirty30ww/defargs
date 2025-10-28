package io.github.thirty30ww.utils;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.VariableElement;
import java.util.Map;

/**
 * 方法生成工具类
 */
public class MethodGenerator {
    
    private final TreeMaker treeMaker;
    private final Names names;
    
    public MethodGenerator(TreeMaker treeMaker, Names names) {
        this.treeMaker = treeMaker;
        this.names = names;
    }
    
    /**
     * 创建重载方法的 AST 节点
     */
    public JCTree.JCMethodDecl createOverloadMethod(JCTree.JCMethodDecl originalMethod,
                                                   java.util.List<? extends VariableElement> parameters,
                                                   Map<Integer, String> defaultValueMap,
                                                   int dropCount) {
        // 复制方法修饰符（public, static 等）
        long modifiers = originalMethod.mods.flags;
        
        // 创建新的参数列表（省略末尾 dropCount 个参数）
        ListBuffer<JCTree.JCVariableDecl> newParams = createNewParameters(originalMethod, parameters, dropCount);
        
        // 创建方法体：调用原方法，补充默认值
        JCTree.JCBlock body = createMethodBody(originalMethod, parameters, defaultValueMap, dropCount);
        
        // 创建方法声明
        return treeMaker.MethodDef(
                treeMaker.Modifiers(modifiers),
                originalMethod.name,
                originalMethod.restype,
                originalMethod.typarams,
                newParams.toList(),
                originalMethod.thrown,
                body,
                null
        );
    }
    
    /**
     * 创建新的参数列表
     */
    private ListBuffer<JCTree.JCVariableDecl> createNewParameters(JCTree.JCMethodDecl originalMethod,
                                                                 java.util.List<? extends VariableElement> parameters,
                                                                 int dropCount) {
        ListBuffer<JCTree.JCVariableDecl> newParams = new ListBuffer<>();
        int keepCount = parameters.size() - dropCount;
        
        // 为新方法创建参数的副本
        for (int i = 0; i < keepCount; i++) {
            JCTree.JCVariableDecl originalParam = originalMethod.params.get(i);
            JCTree.JCVariableDecl newParam = treeMaker.VarDef(
                    treeMaker.Modifiers(originalParam.mods.flags),
                    originalParam.name,
                    originalParam.vartype,
                    null  // 参数没有初始化表达式
            );
            newParams.append(newParam);
        }
        
        return newParams;
    }
    
    /**
     * 创建方法体
     */
    private JCTree.JCBlock createMethodBody(JCTree.JCMethodDecl originalMethod,
                                           java.util.List<? extends VariableElement> parameters,
                                           Map<Integer, String> defaultValueMap,
                                           int dropCount) {
        // 创建方法调用参数
        ListBuffer<JCTree.JCExpression> args = createMethodCallArgs(parameters, defaultValueMap, dropCount);
        
        // 创建方法调用语句
        JCTree.JCExpression methodCall = treeMaker.Apply(
                List.nil(),
                treeMaker.Ident(originalMethod.name),
                args.toList()
        );
        
        // 创建 return 语句（如果方法有返回值）
        JCTree.JCStatement statement = createReturnStatement(originalMethod, methodCall);
        
        // 创建方法体
        return treeMaker.Block(0, List.of(statement));
    }
    
    /**
     * 创建方法调用参数
     */
    private ListBuffer<JCTree.JCExpression> createMethodCallArgs(java.util.List<? extends VariableElement> parameters,
                                                              Map<Integer, String> defaultValueMap,
                                                              int dropCount) {
        ListBuffer<JCTree.JCExpression> args = new ListBuffer<>();
        int keepCount = parameters.size() - dropCount;
        
        // 添加保留的参数
        for (int i = 0; i < keepCount; i++) {
            String paramName = parameters.get(i).getSimpleName().toString();
            args.append(treeMaker.Ident(names.fromString(paramName)));
        }
        
        // 添加默认值参数
        for (int i = keepCount; i < parameters.size(); i++) {
            String defaultValue = defaultValueMap.get(i);
            String paramType = parameters.get(i).asType().toString();
            JCTree.JCExpression defaultExpr = TypeConverter.createDefaultValueExpression(treeMaker, defaultValue, paramType);
            args.append(defaultExpr);
        }
        
        return args;
    }
    
    /**
     * 创建返回语句
     */
    private JCTree.JCStatement createReturnStatement(JCTree.JCMethodDecl originalMethod, JCTree.JCExpression methodCall) {
        if (originalMethod.restype.type.toString().equals("void")) {
            return treeMaker.Exec(methodCall);
        } else {
            return treeMaker.Return(methodCall);
        }
    }
}

