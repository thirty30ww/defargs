# DefArgs

一个 Java 注解处理器，通过 `@DefaultValue` 注解为方法参数提供默认值支持。

## 快速开始

在 Maven 项目中添加依赖：

```xml
<dependency>
    <groupId>io.github.thirty30ww</groupId>
    <artifactId>defargs</artifactId>
    <version>1.0.1</version>
</dependency>
```

## 使用方法

为方法参数添加 `@DefaultValue` 注解，编译器会自动生成对应的重载方法。

```java
public class UserService {
    public void createUser(
        String name,
        @DefaultValue("18") int age,
        @DefaultValue("true") boolean active
    ) {
        // 实现逻辑
    }
}
```

编译后自动生成：

```java
// 生成的重载方法 1
public void createUser(String name, int age) {
    createUser(name, age, true);
}

// 生成的重载方法 2  
public void createUser(String name) {
    createUser(name, 18, true);
}
```

现在你可以用更简洁的方式调用：

```java
service.createUser("Alice");              // 使用全部默认值
service.createUser("Bob", 25);            // 部分使用默认值
service.createUser("Charlie", 30, false); // 不使用默认值
```

## 工作原理

> DefArgs 在编译时通过 Java 注解处理器生成重载方法，不依赖反射，对运行时性能零影响。

注解处理器会扫描所有带 `@DefaultValue` 注解的方法参数，并在编译时直接修改 AST（抽象语法树），将生成的重载方法添加到同一个类中。这意味着生成的代码与手写代码完全相同，没有任何性能开销。

## 注意事项

当前版本仅支持**末尾连续**的默认参数。例如：

```java
// 支持
void method(int a, @DefaultValue("1") int b, @DefaultValue("2") int c)

// 不支持
void method(@DefaultValue("1") int a, int b, @DefaultValue("2") int c)
```

## IntelliJ IDEA 支持

如果你使用 IntelliJ IDEA，建议安装配套的 [defargs-intellij-plugin](https://github.com/thirty30ww/defargs-intellij-plugin) ，以获得更好的开发体验。插件会让 IDEA 识别这些生成的重载方法，消除错误提示并提供代码补全。

## 构建项目

```bash
mvnw clean install
```

## 协议

MIT License

