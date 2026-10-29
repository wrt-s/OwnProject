package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 盲盒抽取记录
 */
@Data
@TableName("blind_box_record")
public class BlindBoxRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long dishId;

    /** 菜品名称（冗余） */
    private String dishName;

    /** 菜品价格（冗余） */
    private Integer dishPrice;

    /** 本次消耗爱心币 */
    private Integer costCoins;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
