package com.wrt.excel.order.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 改密请求。不用 {@code @Data}，避免 toString 带出密码。
 */
@Getter
@Setter
public class PasswordChangeRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
