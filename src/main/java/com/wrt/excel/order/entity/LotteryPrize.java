package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转盘奖品配置实体
 */
@Data
@TableName("lottery_prize")
public class LotteryPrize implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 奖品名称
     */
    private String name;
    
    /**
     * 奖品描述
     */
    private String description;
    
    /**
     * 图标emoji
     */
    private String icon;
    
    /**
     * 奖品类型：coins/blank/thanks
     */
    private String prizeType;
    
    /**
     * 奖品值（爱心币数量）
     */
    private Integer value;
    
    /**
     * 中奖概率（百分比）
     */
    private BigDecimal probability;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 是否启用
     */
    private Integer isActive;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
