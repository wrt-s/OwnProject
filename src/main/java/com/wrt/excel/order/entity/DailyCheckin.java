package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到记录实体
 */
@Data
@TableName("daily_checkin")
public class DailyCheckin implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 签到日期
     */
    private LocalDate checkinDate;
    
    /**
     * 连续签到天数
     */
    private Integer continuousDays;
    
    /**
     * 获得爱心币奖励
     */
    private Integer coinsReward;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /** 兼容前端字段名 coinReward / reward / amount */
    @JsonProperty("coinReward")
    public Integer getCoinReward() { return coinsReward; }

    @JsonProperty("reward")
    public Integer getReward() { return coinsReward; }

    @JsonProperty("amount")
    public Integer getAmount() { return coinsReward; }

}
