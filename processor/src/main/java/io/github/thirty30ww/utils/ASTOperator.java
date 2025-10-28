package io.github.thirty30ww.utils;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * AST 操作工具类
 */
public class ASTOperator {
    
    private final TreeMaker treeMaker;
    private final Names names;
    
    public ASTOperator(Context context) {
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }
    
    /**
     * 在类的 AST 中查找对应的方法节点
     */
    public JCTree.JCMethodDecl findMethodTree(JCTree.JCClassDecl classTree, ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        java.util.List<? extends VariableElement> methodParams = method.getParameters();
        
        for (JCTree def : classTree.defs) {
            if (def instanceof JCTree.JCMethodDecl methodDecl) {
                if (methodDecl.name.toString().equals(methodName) &&
                    methodDecl.params.size() == methodParams.size()) {
                    
                    if (isParameterTypesMatch(methodParams, methodDecl)) {
                        return methodDecl;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 检查参数类型是否匹配
     */
    private boolean isParameterTypesMatch(java.util.List<? extends VariableElement> methodParams, 
                                        JCTree.JCMethodDecl methodDecl) {
        for (int i = 0; i < methodParams.size(); i++) {
            String expectedType = methodParams.get(i).asType().toString();
            String actualType = methodDecl.params.get(i).vartype.type.toString();
            
            if (!simplifyTypeName(expectedType).equals(simplifyTypeName(actualType))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查类中是否已存在指定签名的方法
     */
    public boolean methodExists(JCTree.JCClassDecl classTree, String methodName,
                              JCTree.JCMethodDecl originalMethod, int paramCount) {
        for (JCTree def : classTree.defs) {
            if (def instanceof JCTree.JCMethodDecl methodDecl) {
                if (methodDecl == originalMethod) {
                    continue;
                }
                
                if (methodDecl.name.toString().equals(methodName) &&
                    methodDecl.params.size() == paramCount) {
                    
                    if (isParameterTypesMatch(originalMethod, methodDecl, paramCount)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 检查两个方法的参数类型是否匹配
     */
    private boolean isParameterTypesMatch(JCTree.JCMethodDecl originalMethod, 
                                        JCTree.JCMethodDecl methodDecl, int paramCount) {
        for (int i = 0; i < paramCount && i < originalMethod.params.size(); i++) {
            String originalType = originalMethod.params.get(i).vartype.type.toString();
            String existingType = methodDecl.params.get(i).vartype.type.toString();
            
            if (!simplifyTypeName(originalType).equals(simplifyTypeName(existingType))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 简化类型名称，去除包名和泛型信息
     */
    public String simplifyTypeName(String typeName) {
        // 去除泛型信息
        int genericIndex = typeName.indexOf('<');
        if (genericIndex > 0) {
            typeName = typeName.substring(0, genericIndex);
        }
        
        // 去除包名
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot > 0) {
            typeName = typeName.substring(lastDot + 1);
        }
        
        return typeName;
    }
    
    /**
     * 获取 TreeMaker 实例
     */
    public TreeMaker getTreeMaker() {
        return treeMaker;
    }
    
    /**
     * 获取 Names 实例
     */
    public Names getNames() {
        return names;
    }
}
