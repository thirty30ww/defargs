import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

public class ASTTreeViewer {
    public static void main(String[] args) {
        String code = """
                public class Calculator {
                    private int value;
                    public int add (int a, int b) {
                        return a + b + value;
                    }
                }
                """;
        
        CompilationUnit cu = StaticJavaParser.parse(code);
        
        System.out.println("=== AST树状结构 ===");
        printASTTree(cu, 0);
    }
    
    /**
     * 递归打印AST树结构
     * @param node 当前节点
     * @param depth 当前深度（用于缩进）
     */
    public static void printASTTree(Node node, int depth) {
        String indent = "  ".repeat(depth); // 根据深度生成缩进
        String nodeType = node.getClass().getSimpleName();
        
        // 简化的节点内容（避免输出过长）
        String content = getNodeSummary(node);
        
        System.out.println(indent + nodeType + ": " + content);
        
        // 递归打印所有子节点
        for (Node child : node.getChildNodes()) {
            printASTTree(child, depth + 1);
        }
    }
    
    /**
     * 获取节点的简化信息
     */
    private static String getNodeSummary(Node node) {
        String fullString = node.toString().split("\n")[0]; // 取第一行
        if (fullString.length() > 50) {
            return fullString.substring(0, 47) + "...";
        }
        return fullString;
    }
}