package com.cby.networklib.annotation;


import com.cby.networklib.type.NetType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 作用、目标：{ElementType.FIELD 在属性之上； ElementType.METHOD 在方法之上；ElementType.TYPE 在类、接口之上}
 * jvm 在 {RetentionPolicy.CLASS 源码；RetentionPolicy.SOURCE 编译期；RetentionPolicy.RUNTIME 运行时} ，通过反射获取注解的值
 */
@Target(ElementType.METHOD)             // 作用、目标在方法之上
@Retention(RetentionPolicy.RUNTIME)     // jvm 在运行时，通过反射获取注解的值
public @interface Network {

    NetType netType() default NetType.AUTO; // 默认为 NetType.AUTO
}
