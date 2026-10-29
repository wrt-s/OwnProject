package com.wrt.excel.order.dto;

import lombok.Data;
import java.util.List;

/**
 * 下单请求DTO
 */
@Data
public class OrderRequestDTO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 购物车项列表
     */
    private List<CartItemDTO> items;
    
    /**
     * 订单备注
     */
    private String remark;
}
