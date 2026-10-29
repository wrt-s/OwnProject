package com.wrt.excel.order.dto;

import lombok.Data;
import java.util.List;

/**
 * 购物车项DTO
 */
@Data
public class CartItemDTO {
    
    /**
     * 菜品ID
     */
    private Long dishId;
    
    /**
     * 数量
     */
    private Integer quantity;
    
    /**
     * 备注
     */
    private String remark;
}
