package com.wrt.excel.system.exception;

/**
 * @author: 卫瑞涛
 * @version: V1.0
 * @data: 2026/03/25 10:03
 * @description: 远程服务异常
 **/
public class RemoteServiceException extends RuntimeException {

    public RemoteServiceException(String message) {
        super(message);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}