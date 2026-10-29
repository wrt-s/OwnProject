package com.wrt.excel.system.exception;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

/**
 * Controllers historically returned HTTP 200 with {@code code: 500} in the body.
 * When the body carries an error code, mirror it onto the HTTP status.
 */
@ControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Map<?, ?> map) {
            Object code = map.get("code");
            if (code instanceof Number number) {
                int status = number.intValue();
                if (status >= 400 && status <= 599) {
                    response.setStatusCode(HttpStatusCode.valueOf(status));
                }
            }
        }
        return body;
    }
}
