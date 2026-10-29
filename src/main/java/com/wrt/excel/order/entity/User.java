package com.wrt.excel.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体（情侣双角色：ORDERER 点单方 / CHEF 做餐方）
 */
@Data
@TableName("user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nickname;

    private String avatarUrl;

    private String themeColor;

    private LocalDate birthday;

    /** 手机号（登录账号） */
    private String phone;

    /** 密码（MD5 存储） */
    @JsonIgnore
    private String password;

    /**
     * 角色：ORDERER=点单方，CHEF=做餐方
     * 兼容旧值 USER→ORDERER，ADMIN→CHEF
     */
    private String role;

    /** 家庭/情侣共同空间 ID */
    private Long familyId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime lastLoginTime;

    private LocalDate periodStartDate;

    private Integer periodCycleDays;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;

    public static final String ROLE_ORDERER = "ORDERER";
    public static final String ROLE_CHEF = "CHEF";

    /** 规范化角色（兼容旧 USER/ADMIN） */
    public String normalizedRole() {
        if (role == null) return ROLE_ORDERER;
        String r = role.trim().toUpperCase();
        if ("ADMIN".equals(r) || ROLE_CHEF.equals(r)) return ROLE_CHEF;
        if ("USER".equals(r) || ROLE_ORDERER.equals(r)) return ROLE_ORDERER;
        return r;
    }

    public boolean isChef() {
        return ROLE_CHEF.equals(normalizedRole());
    }

    public boolean isOrderer() {
        return ROLE_ORDERER.equals(normalizedRole());
    }
}
