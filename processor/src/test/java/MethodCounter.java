import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class MethodCounter extends VoidVisitorAdapter<Void> {
    private int methodCount = 0;
    
    @Override
    public void visit(MethodDeclaration md, Void arg) {
        methodCount++;
        System.out.println("发现方法: " + md.getName() + 
                         " (参数: " + md.getParameters().size() + ")");
        super.visit(md, arg);
    }
    
    public int getMethodCount() {
        return methodCount;
    }
}