package org.malred.mvcframework.annotations;

import java.lang.annotation.*;

@Documented // 可以被java文档工具扫描到
@Target(ElementType.TYPE) // 该注解应标记在类上
@Retention(RetentionPolicy.RUNTIME) // 运行时也生效
public @interface iRestController {
    String value() default "";
}
