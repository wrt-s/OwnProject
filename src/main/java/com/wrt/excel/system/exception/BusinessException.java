package com.wrt.excel.system.exception;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 10:03
 * @description: 业务异常
 **/
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}