package io.github.thirty30ww;

import io.github.thirty30ww.annotation.DefaultValue;

/**
 * 示例服务：展示为方法参数提供默认值的效果。
 * 处理器会生成一个派生类 ExampleServiceOverloaded，
 * 其中包含省略 b 参数的重载：add(String a) => add(a, "World")。
 */
public class ExampleService {
    public String add(String a, @DefaultValue("World") String b) {
        return a + b;
    }

    public String threeArgs(String a, @DefaultValue("B") String b, @DefaultValue("C") String c) {
        return a + b + c;
    }

    public Integer add(@DefaultValue("1") Integer a, @DefaultValue("2") Integer b) {
        return a + b;
    }

    public Boolean isTrue(@DefaultValue("true") Boolean b) {
        return b;
    }

    public boolean isFalse(@DefaultValue("false") boolean b) {
        return !b;
    }
}