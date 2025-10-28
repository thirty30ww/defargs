import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class GetterGenerator extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
        // 为每个字段生成getter
        cid.getFields().forEach(field -> {
            field.getVariables().forEach(variable -> {
                String fieldName = variable.getNameAsString();
                String fieldType = field.getElementType().asString();
                generateGetter(cid, fieldName, fieldType);
            });
        });
        super.visit(cid, arg);
    }
    
    private void generateGetter(ClassOrInterfaceDeclaration clazz,
                                String fieldName, String fieldType) {
        String getterName = "get" + capitalize(fieldName);
        String getterCode = String.format(
            "public %s %s() { return this.%s; }",
            fieldType, getterName, fieldName
        );
        
        try {
            var getter = StaticJavaParser.parseBodyDeclaration(getterCode);
            clazz.addMember(getter);
            System.out.println("生成Getter: " + getterName);
        } catch (Exception e) {
            System.err.println("生成Getter失败: " + e.getMessage());
        }
    }
    
    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}