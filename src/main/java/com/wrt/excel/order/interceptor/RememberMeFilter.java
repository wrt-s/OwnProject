package com.wrt.excel.order.interceptor;

import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.RememberMeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 无有效 Session 但有有效 LOVE_REMEMBER cookie 时，自动重建登录 Session。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class RememberMeFilter extends OncePerRequestFilter {

    private final RememberMeService rememberMeService;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            HttpSession existing = request.getSession(false);
            if (AuthService.sessionUserId(existing) == null) {
                Long uid = rememberMeService.resolveUserId(request);
                if (uid != null) {
                    User user = authService.loadUserById(uid);
                    if (user != null) {
                        HttpSession session = request.getSession(true);
                        authService.bindSession(user, session);
                        session.setMaxInactiveInterval(RememberMeService.REMEMBER_SECONDS);
                        log.debug("Remember-me restored userId={}", uid);
                    } else {
                        rememberMeService.clear(response);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Remember-me restore skipped: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
