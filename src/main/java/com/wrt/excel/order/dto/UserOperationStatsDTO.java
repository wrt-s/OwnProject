package com.wrt.excel.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户操作统计DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserOperationStatsDTO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 操作次数
     */
    private Long operationCount;
    
    /**
     * 成功次数
     */
    private Long successCount;
    
    /**
     * 失败次数
     */
    private Long failedCount;
}
