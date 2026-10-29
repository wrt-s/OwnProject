package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 赚币任务定义（点单方完成可获爱心币）
 */
@Data
@TableName("reward_task")
public class RewardTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    /** 任务名称，如「爱爱」 */
    private String name;

    private String description;

    /** 奖励爱心币数量 */
    private Integer coinReward;

    /** 是否启用 */
    private Integer enabled;

    /** 是否需要做餐方确认后才发币：0=完成即发，1=需确认 */
    private Integer requireConfirm;

    /** 每日可完成次数，0=不限制 */
    private Integer dailyLimit;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
