package com.wrt.excel.order.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String phone;
    private String password;
    /** 记住登录：默认 true，签发长效 remember cookie */
    private Boolean rememberMe;
}
