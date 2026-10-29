package com.wrt.excel.order.dto;

import lombok.Data;

/**
 * 操作日志统计DTO
 */
@Data
public class OperationLogStatsDTO {
    
    /**
     * 总操作数
     */
    private Long totalOperations;
    
    /**
     * 成功操作数
     */
    private Long successOperations;
    
    /**
     * 失败操作数
     */
    private Long failedOperations;
    
    /**
     * 成功率
     */
    private Double successRate;
    
    /**
     * 平均耗时（毫秒）
     */
    private Double avgCostTime;
    
    /**
     * 最大耗时（毫秒）
     */
    private Long maxCostTime;
    
    /**
     * 最小耗时（毫秒）
     */
    private Long minCostTime;
}
