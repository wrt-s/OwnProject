package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@TableName("operation_log")
public class OperationLog {
    
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 操作模块
     */
    private String module;
    
    /**
     * 操作类型（新增/修改/删除/查询等）
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 请求方法（类名.方法名）
     */
    private String method;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * 请求方法（GET/POST/PUT/DELETE）
     */
    private String requestMethod;
    
    /**
     * 请求参数
     */
    private String requestParams;
    
    /**
     * 响应结果
     */
    private String responseResult;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 操作耗时（毫秒）
     */
    private Long costTime;
    
    /**
     * 操作状态（0-失败 1-成功）
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime operationTime;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
