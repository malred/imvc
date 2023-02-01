package org.malred.mvcframework.annotations;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.METHOD}) // 可以添加到类和方法上
@Retention(RetentionPolicy.RUNTIME)
public @interface iRequestMapping {
    String value() default "";
}
