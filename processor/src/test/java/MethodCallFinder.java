import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

public class MethodCallFinder extends VoidVisitorAdapter<Void> {
    private final String targetMethod;
    private final List<String> calls = new ArrayList<>();
    
    public MethodCallFinder(String targetMethod) {
        this.targetMethod = targetMethod;
    }
    
    @Override
    public void visit(MethodCallExpr n, Void arg) {
        if (n.getNameAsString().equals(targetMethod)) {
            calls.add("在位置: " + n.getRange().map(r -> r.begin.line).orElse(-1));
        }
        super.visit(n, arg);
    }

    public List<String> getCalls() {
        return calls;
    }
}