package com.wrt.excel.system.exception;

import java.util.Map;

/**
 * Map business failures to 4xx codes stored in the API body.
 * {@link ApiResponseAdvice} copies that code onto the HTTP status.
 */
public final class ApiErrors {

    private ApiErrors() {}

    public static void fill(Map<String, Object> result, Exception e) {
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            String message = e.getMessage() == null || e.getMessage().isBlank() ? "请求无效" : e.getMessage();
            result.put("code", statusForMessage(message));
            result.put("message", message);
            return;
        }
        if (e instanceof NullPointerException) {
            result.put("code", 400);
            result.put("message", "参数不完整");
            return;
        }
        result.put("code", 500);
        String message = e.getMessage();
        result.put("message", message == null || message.isBlank() ? "服务器繁忙，请稍后重试" : message);
    }

    public static int statusForMessage(String message) {
        if (message == null) return 400;
        if (message.contains("请先登录") || message.contains("未登录")) return 401;
        if (message.contains("无权") || message.contains("仅做餐")) return 403;
        if (message.contains("不存在")) return 404;
        return 400;
    }
}
