package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 纪念日实体
 */
@Data
@TableName("anniversary")
public class Anniversary implements Serializable {
    
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
     * 纪念日名称
     */
    private String name;
    
    /**
     * 日期
     */
    private LocalDate date;
    
    /**
     * 类型：birthday/first_date/wedding/custom
     */
    private String type;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 提前提醒天数
     */
    private Integer reminderDays;
    
    /**
     * 是否启用
     */
    private Integer isActive;
    
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
