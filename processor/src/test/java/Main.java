import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class Main {
    public static void main(String[] args) {
        String code = """
        public class Test {
            private int a;
            public String b;
        
            public void test() {
                String c;
                a = 1;
                b = "2";
            }
        
            public Integer getTest() {
                test();
                return a;
            }
        }
        """;
        // 解析代码为 AST
        CompilationUnit cu = StaticJavaParser.parse(code);

        System.out.println("=== 修改前 ===");
        System.out.println(cu.toString());

        ClassModifier.addField(cu, "id", "int");
        ClassModifier.addField(cu, "name", "String");

        System.out.println("\n=== 修改后 ===");
        System.out.println(cu);
    }
}
