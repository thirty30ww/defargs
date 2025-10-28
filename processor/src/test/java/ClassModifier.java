import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class ClassModifier {
    public static void addField(CompilationUnit cu,
                                String fieldName,
                                String fieldType) {
        cu.getClassByName("Test").ifPresent(clazz -> {
            // 构建字段代码
            String fieldCode = "private " + fieldType + " " + fieldName + ";";

            // 解析为AST节点并添加
            var field = StaticJavaParser.parseBodyDeclaration(fieldCode);
            clazz.addMember(field);

            System.out.println("✅ 成功添加字段: " + fieldName);
        });
    }
}