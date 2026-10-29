package com.wrt.excel.order.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wrt.excel.order.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 简易鉴权：
 * - 公开：登录、菜品/分类只读、静态资源
 * - 需登录：下单、取消、玩法、币操作等（业务层再用 Session 覆盖 client userId）
 * - 做餐方 CHEF：订单状态变更、看板、操作日志、加币；菜品/分类写对点单方开放
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    /** 完全公开的 API 前缀/路径 */
    private static final Set<String> PUBLIC_EXACT = Set.of(
            "/api/auth/login",
            "/api/auth/me",
            "/api/wechat/callback"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 非 /api 由页面路由/静态资源处理
        // /admin：登录即可（点单方也可维护菜品）；/dashboard：仅做餐方
        if (!path.startsWith("/api/")) {
            if (path.equals("/admin") || path.startsWith("/admin/")) {
                HttpSession session = request.getSession(false);
                if (session == null || AuthService.sessionUserId(session) == null) {
                    response.sendRedirect("/login");
                    return false;
                }
                return true;
            }
            if (path.equals("/dashboard") || path.startsWith("/dashboard/")
                    || path.equals("/family-location") || path.startsWith("/family-location/")) {
                HttpSession session = request.getSession(false);
                if (session == null || AuthService.sessionUserId(session) == null) {
                    response.sendRedirect("/login");
                    return false;
                }
                if (!AuthService.isChefSession(session)) {
                    response.sendRedirect("/?denied=chef");
                    return false;
                }
            }
            return true;
        }

        if (isPublicApi(path, method)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Long userId = session == null ? null : AuthService.sessionUserId(session);
        if (userId == null) {
            writeJson(response, 401, "请先登录");
            return false;
        }

        // 挂到 request，便于 Controller 兜底读取
        request.setAttribute("LOVE_SESSION_USER_ID", userId);
        request.setAttribute("LOVE_SESSION_ROLE", AuthService.sessionRole(session));

        if (requiresChef(path, method) && !AuthService.isChefSession(session)) {
            writeJson(response, 403, "仅做餐方可以操作");
            return false;
        }

        return true;
    }

    private boolean isPublicApi(String path, String method) {
        if (PUBLIC_EXACT.contains(path)) return true;
        if (path.startsWith("/api/wechat/")) return true;
        if (path.equals("/api/auth/logout")) return true; // 允许未登录调用登出

        // 菜品只读
        if ("GET".equalsIgnoreCase(method)) {
            if (path.equals("/api/dish/list")
                    || path.equals("/api/dish/all")
                    || path.equals("/api/dish/recommended")
                    || path.equals("/api/dish/new")
                    || path.equals("/api/dish/signature")
                    || path.equals("/api/dish/surprise")
                    || path.equals("/api/dish/filter")
                    || path.startsWith("/api/dish/category/")
                    || path.equals("/api/category/list")
                    || path.equals("/api/category/all")) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresChef(String path, String method) {
        // 订单状态 & 全量订单 & 看板
        if (path.equals("/api/order/status") || path.equals("/api/order/list")) return true;
        if (path.startsWith("/api/order/dashboard")) return true;

        // 菜品/分类写操作：点单方与做餐方均可（情侣共同维护菜单）
        // （不再 requireChef）

        // 给他人加币仅做餐方
        if (path.equals("/api/coin/add")) {
            return true;
        }

        // 操作日志等后台
        if (path.startsWith("/api/operation-log") || path.startsWith("/api/operationLog")) {
            return true;
        }
        // 点单方在线状态仅做餐方
        if (path.equals("/api/online/partner") || path.startsWith("/api/online/partner/")) {
            return true;
        }
        // 家人位置列表仅做餐方
        if (path.equals("/api/family-location/list") || path.startsWith("/api/family-location/list/")) {
            return true;
        }
        // 赚币任务：管理/确认仅做餐方
        if (path.equals("/api/reward-task/pending")
                || path.equals("/api/reward-task/confirm")
                || path.equals("/api/reward-task/save")
                || path.equals("/api/reward-task/toggle")
                || path.equals("/api/reward-task/manage/list")
                || path.startsWith("/api/reward-task/manage/")) {
            return true;
        }
        return false;
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code == 401 ? 401 : (code == 403 ? 403 : 200));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
