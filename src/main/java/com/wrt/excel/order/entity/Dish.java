package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品实体
 */
@Data
@TableName("dish")
public class Dish implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 菜品名称
     */
    private String name;
    
    /**
     * 菜品描述
     */
    private String description;
    
    /**
     * 图片URL — ALWAYS so clearing to null/empty persists (default NOT_NULL skips null)
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String imageUrl;
    
    /**
     * 价格类型：爱心币/抱抱/亲亲等
     */
    private String priceType;
    
    /**
     * 价格数量
     */
    private Integer price;
    
    /**
     * 是否拿手菜
     */
    private Integer isSignature;
    
    /**
     * 是否新品
     */
    private Integer isNew;
    
    /**
     * 是否今日推荐
     */
    private Integer isRecommended;
    
    /**
     * 是否售罄
     */
    private Integer isSoldOut;
    
    /**
     * 标签，逗号分隔
     */
    private String tags;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
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
