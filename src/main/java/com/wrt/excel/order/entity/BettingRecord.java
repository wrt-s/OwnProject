package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 幸运币押注记录实体
 */
@Data
@TableName("betting_record")
public class BettingRecord implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 下注金额
     */
    private Integer betAmount;
    
    /**
     * 押注类型：big/small
     */
    private String betType;
    
    /**
     * 骰子结果（1-6）
     */
    private Integer diceResult;
    
    /**
     * 结果类型：big/small
     */
    private String resultType;
    
    /**
     * 是否获胜：0-失败，1-胜利
     */
    private Integer isWin;
    
    /**
     * 奖励金额（获胜时）
     */
    private Integer rewardAmount;
    
    /**
     * 扣除金额
     */
    private Integer deductAmount;
    
    /**
     * 是否半扣：0-正常扣，1-只扣一半
     */
    private Integer isHalfDeduct;
    
    /**
     * 连续输的次数
     */
    private Integer consecutiveLoss;
    
    /**
     * 当日累计损失
     */
    private Integer dailyTotalLoss;
    
    /**
     * 押注时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /** 历史列表兼容字段：点数 */
    @JsonProperty("dice")
    public Integer getDice() {
        return diceResult;
    }

    @JsonProperty("point")
    public Integer getPoint() {
        return diceResult;
    }

    /** 历史列表兼容字段：赢的派彩（不是 0） */
    @JsonProperty("payout")
    public Integer getPayout() {
        return rewardAmount;
    }

    @JsonProperty("winAmount")
    public Integer getWinAmount() {
        return rewardAmount;
    }

    @JsonProperty("win")
    public Boolean getWin() {
        return isWin != null && isWin == 1;
    }
}
