package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 爱心币流水实体
 */
@Data
@TableName("coin_transaction")
public class CoinTransaction implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 变动数量（正数为增加，负数为减少）
     */
    private Integer amount;
    
    /**
     * 变动后余额
     */
    private Integer balanceAfter;
    
    /**
     * 类型：add_坏坏/use_order/use_task
     */
    private String type;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 关联ID（订单ID或任务记录ID）
     */
    private Long relatedId;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
