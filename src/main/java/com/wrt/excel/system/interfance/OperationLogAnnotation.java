package com.wrt.excel.system.interfance;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogAnnotation {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型（新增/修改/删除/查询等）
     */
    String operationType() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean recordResult() default false;
}
