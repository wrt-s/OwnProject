package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 喂食大作战游戏记录实体
 */
@Data
@TableName("feeding_game_record")
public class FeedingGameRecord implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 游戏得分
     */
    private Integer score;
    
    /**
     * 游戏时长（秒）
     */
    private Integer duration;
    
    /**
     * 吃到的食物数量
     */
    private Integer foodCount;
    
    /**
     * 吃到的黑暗料理数量
     */
    private Integer darkFoodCount;
    
    /**
     * 解锁的特殊效果
     */
    private String unlockedEffect;
    
    /**
     * 游戏时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
