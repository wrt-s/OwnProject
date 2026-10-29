package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家人位置 - 每用户每家庭一条最新上报
 */
@Data
@TableName("family_member_location")
public class FamilyMemberLocation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    private Long userId;

    /** 上报时昵称快照 */
    private String nickname;

    /** 上报时角色 ORDERER / CHEF */
    private String role;

    private Double lat;

    private Double lng;

    /** 精度（米） */
    private Double accuracy;

    /** 位置名称 / POI（高德逆地理） */
    private String placeName;

    /** 街道 / 门牌 */
    private String street;

    /** 完整地址 formatted_address */
    private String address;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}