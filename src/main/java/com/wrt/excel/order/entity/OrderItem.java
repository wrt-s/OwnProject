package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单明细实体
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 菜品ID
     */
    private Long dishId;
    
    /**
     * 菜品名称（冗余）
     */
    private String dishName;
    
    /**
     * 数量
     */
    private Integer quantity;
    
    /**
     * 单价
     */
    private Integer price;
    
    /**
     * 备注（少冰、不放香菜等）
     */
    private String remark;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
