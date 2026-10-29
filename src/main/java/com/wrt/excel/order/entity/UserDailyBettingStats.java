package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日押注统计实体
 */
@Data
@TableName("user_daily_betting_stats")
public class UserDailyBettingStats implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 押注日期
     */
    private LocalDate betDate;
    
    /**
     * 总押注次数
     */
    private Integer totalBets;
    
    /**
     * 获胜次数
     */
    private Integer winCount;
    
    /**
     * 失败次数
     */
    private Integer lossCount;
    
    /**
     * 总下注金额
     */
    private Integer totalBetAmount;
    
    /**
     * 总奖励金额
     */
    private Integer totalReward;
    
    /**
     * 总损失金额
     */
    private Integer totalLoss;
    
    /**
     * 当前连续输的次数
     */
    private Integer consecutiveLoss;
    
    /**
     * 是否被锁定：0-未锁定，1-已锁定
     */
    private Integer isLocked;
    
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
