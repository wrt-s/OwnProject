package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成就实体类
 */
@Data
@TableName("achievement")
public class Achievement {
    
    /**
     * 成就ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 成就名称
     */
    private String name;
    
    /**
     * 成就描述
     */
    private String description;
    
    /**
     * 图标emoji
     */
    private String icon;
    
    /**
     * 条件类型：consecutive_days/try_all_dishes/order_count/special_dish
     */
    private String conditionType;
    
    /**
     * 条件值
     */
    private Integer conditionValue;
    
    /**
     * 奖励描述
     */
    private String reward;
    
    /**
     * 奖励爱心币数量
     */
    private Integer rewardCoins;
    
    /**
     * 是否启用
     */
    private Integer isActive;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
