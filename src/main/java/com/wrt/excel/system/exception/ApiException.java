package com.wrt.excel.system.exception;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 10:02
 * @description: API异常
 **/
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}