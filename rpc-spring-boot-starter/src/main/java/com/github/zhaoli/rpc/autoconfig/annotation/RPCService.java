package com.github.zhaoli.rpc.autoconfig.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by zhaoli on 2017/7/31.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RPCService {
    Class<?> interfaceClass() default void.class;
    boolean callback() default false;
    String callbackMethod() default "";
    int callbackParamIndex() default 1;
}
