package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * App / H5 站内通知记录（与微信模板并行，互不影响）。
 */
@Data
@TableName("app_notification")
public class AppNotification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String body;

    /** NEW_ORDER / STATUS / CANCEL */
    private String type;

    private String orderNo;

    /** 0=未读 1=已读 */
    private Integer readFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
