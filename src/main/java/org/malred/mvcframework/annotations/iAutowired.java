package org.malred.mvcframework.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD) // 加在字段上
@Retention(RetentionPolicy.RUNTIME)
public @interface iAutowired {
    String value() default "";
}
