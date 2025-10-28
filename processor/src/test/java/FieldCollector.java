import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

public class FieldCollector extends VoidVisitorAdapter<Void> {
    private final List<String> fields = new ArrayList<>();
    
    @Override 
    public void visit(FieldDeclaration fd, Void arg) {
        fd.getVariables().forEach(variable -> {
            String fieldInfo = String.format("%s %s %s", 
                fd.getModifiers(), // 访问修饰符
                fd.getElementType(), // 字段类型
                variable.getName() // 字段名
            );
            fields.add(fieldInfo);
        });
        super.visit(fd, arg);
    }
    
    public List<String> getFields() {
        return fields;
    }
}