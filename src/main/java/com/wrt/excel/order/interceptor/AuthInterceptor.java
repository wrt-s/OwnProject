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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 简易鉴权：
 * - 公开：登录、菜品/分类只读、静态资源
 * - 需登录：下单、取消、玩法、币操作等（业务层再用 Session 覆盖 client userId）
 * - 做餐方 CHEF：订单状态变更、看板、操作日志、加币、菜品写
 * - 点单方：菜品只读；看板返回 403 页面
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

        // 非 /api：页面必须登录（地址栏跳到 /login）；看板/家人位置仅做餐方
        if (!path.startsWith("/api/")) {
            if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")) {
                return true;
            }
            if (path.startsWith("/actuator")) {
                writeJson(response, 401, "未授权");
                return false;
            }
            HttpSession session = request.getSession(false);
            if (session == null || AuthService.sessionUserId(session) == null) {
                String query = request.getQueryString();
                String target = path + (query == null || query.isBlank() ? "" : "?" + query);
                String next = URLEncoder.encode(target, StandardCharsets.UTF_8);
                response.sendRedirect("/login?redirect=" + next);
                return false;
            }
            if (isChefOnlyPage(path) && !AuthService.isChefSession(session)) {
                writeHtmlDenied(response, chefDenyMessage(path));
                return false;
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

    private boolean isChefOnlyPage(String path) {
        return path.equals("/dashboard") || path.startsWith("/dashboard/")
                || path.equals("/family-location") || path.startsWith("/family-location/");
    }

    private String chefDenyMessage(String path) {
        if (path.startsWith("/family-location")) {
            return "仅做餐方可查看家人位置";
        }
        return "仅做餐方可查看数据看板";
    }

    private boolean requiresChef(String path, String method) {
        // 订单状态 & 全量订单 & 看板
        if (path.equals("/api/order/status") || path.equals("/api/order/list")) return true;
        if (path.startsWith("/api/order/dashboard")) return true;

        // 菜品写：仅做餐方。点单方只读。
        if (isDishWrite(path, method)) return true;

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

    private boolean isDishWrite(String path, String method) {
        if (method == null || "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return false;
        }
        return path.equals("/api/dish/add")
                || path.equals("/api/dish/update")
                || path.equals("/api/dish/toggleSoldOut")
                || path.equals("/api/dish/uploadImage")
                || path.equals("/api/dish/clearImage")
                || path.startsWith("/api/dish/delete/")
                || (path.startsWith("/api/dish/") && path.endsWith("/image"));
    }

    private void writeHtmlDenied(HttpServletResponse response, String message) throws IOException {
        response.setStatus(403);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html;charset=UTF-8");
        String safe = message == null ? "无权访问" : message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        response.getWriter().write("""
                <!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>无权访问 · Love</title>
                <style>
                body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;
                background:#FCE8EE;color:#2C292A;font-family:-apple-system,BlinkMacSystemFont,"PingFang SC",sans-serif}
                .card{background:#fff;border:1px solid #F3D5DC;border-radius:18px;padding:28px 22px;max-width:360px;text-align:center}
                h1{font-size:18px;margin:8px 0} p{font-size:14px;line-height:1.5;color:#6b6466}
                a{display:inline-block;margin-top:16px;background:#E85A7A;color:#fff;text-decoration:none;
                padding:10px 18px;border-radius:999px;font-weight:700}
                </style></head><body><div class="card"><div style="font-size:40px">🚫</div>
                <h1>无权访问</h1><p>%s</p><a href="/">返回首页</a></div></body></html>
                """.formatted(safe));
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
