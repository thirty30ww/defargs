// 静态导入生成的方法

import io.github.thirty30ww.ExampleServiceOverloaded;

public class Main {
    public static void main(String[] args) {
        // 直接使用生成的派生类，以便调用新增的重载方法 add(String)
        ExampleServiceOverloaded exampleService = new ExampleServiceOverloaded();
        System.out.println(exampleService.add("Hello "));
        System.out.println(exampleService.threeArgs("A"));
        System.out.println(exampleService.threeArgs("A", "B"));
        System.out.println(exampleService.isTrue());
        System.out.println(exampleService.isFalse());
        System.out.println(exampleService.add(1));
    }
}
