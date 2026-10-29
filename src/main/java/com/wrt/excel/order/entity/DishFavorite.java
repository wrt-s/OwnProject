package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品收藏实体
 */
@Data
@TableName("dish_favorite")
public class DishFavorite implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 家庭/情侣共同空间 ID（同家庭成员共享读写）
     */
    private Long familyId;
    
    /**
     * 菜品ID
     */
    private Long dishId;
    
    /**
     * 收藏时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
