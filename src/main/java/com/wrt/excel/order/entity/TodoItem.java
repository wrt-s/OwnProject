package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭待办事项实体
 */
@Data
@TableName("todo_item")
public class TodoItem implements Serializable {
    
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
     * 标题
     */
    private String title;
    
    /**
     * 详细描述
     */
    private String description;
    
    /**
     * 优先级：high/medium/low
     */
    private String priority;
    
    /**
     * 状态：pending/in_progress/completed
     */
    private String status;
    
    /**
     * 负责人：布布/佳宝/一起
     */
    private String assignee;
    
    /**
     * 截止日期
     */
    private LocalDate dueDate;
    
    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime completedAt;
    
    /**
     * 是否开启提醒
     */
    private Boolean reminderEnabled;
    
    /**
     * 提醒时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime reminderTime;
    
    /**
     * 提醒状态：pending/sent/cancelled
     */
    private String reminderStatus;
    
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
