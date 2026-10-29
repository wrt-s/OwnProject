package com.wrt.excel.system.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API failures return JSON instead of the Whitelabel HTML error page.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException e) {
        return body(400, "参数格式不正确");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> missingParam(MissingServletRequestParameterException e) {
        return body(400, "缺少参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        return body(400, "请求体无法解析");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> illegalArg(IllegalArgumentException e) {
        String message = e.getMessage() == null || e.getMessage().isBlank() ? "请求无效" : e.getMessage();
        return body(ApiErrors.statusForMessage(message), message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> illegalState(IllegalStateException e) {
        String message = e.getMessage() == null || e.getMessage().isBlank() ? "请求无效" : e.getMessage();
        return body(ApiErrors.statusForMessage(message), message);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> npe(NullPointerException e) {
        log.warn("Missing parameter caused NPE", e);
        return body(400, "参数不完整");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> noResource(NoResourceFoundException e, HttpServletRequest request) {
        String uri = request == null ? "" : request.getRequestURI();
        if (uri != null && (uri.startsWith("/api/") || uri.startsWith("/actuator"))) {
            return body(404, "接口不存在");
        }
        return body(HttpStatus.NOT_FOUND.value(), "页面不存在");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> fallback(Exception e) {
        log.error("Unhandled API error", e);
        return body(500, "服务器繁忙，请稍后重试");
    }

    private static ResponseEntity<Map<String, Object>> body(int code, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("message", message);
        return ResponseEntity.status(code).body(map);
    }
}
