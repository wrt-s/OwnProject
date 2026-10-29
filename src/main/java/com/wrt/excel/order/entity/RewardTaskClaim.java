package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 赚币任务完成/领取记录
 */
@Data
@TableName("reward_task_claim")
public class RewardTaskClaim implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long userId;

    private Long familyId;

    /** pending / approved / rejected / completed */
    private String status;

    private Integer coinsAwarded;

    private String remark;

    private Long confirmedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime confirmedAt;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /** 非表字段：任务名 */
    @TableField(exist = false)
    private String taskName;

    @TableField(exist = false)
    private String userNickname;
}
