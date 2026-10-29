package com.wrt.excel.order.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜品展示VO
 */
@Data
public class DishVO {
    
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String imageUrl;
    private String priceType;
    private Integer price;
    private Integer isSignature;
    private Integer isNew;
    private Integer isRecommended;
    private Integer isSoldOut;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
