package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 抽奖记录实体
 */
@Data
@TableName("lottery_record")
public class LotteryRecord implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 奖品ID
     */
    private Long prizeId;
    
    /**
     * 奖品名称（冗余）
     */
    private String prizeName;
    
    /**
     * 奖品类型
     */
    private String prizeType;
    
    /**
     * 奖品值
     */
    private Integer prizeValue;
    
    /**
     * 消耗爱心币数量
     */
    private Integer costCoins;
    
    /**
     * 抽奖时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
