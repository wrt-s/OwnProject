package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.Notebook;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.DeepSeekService;
import com.wrt.excel.order.service.NotebookService;
import com.wrt.excel.system.interfance.OperationLogAnnotation;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 一句话助手 → 点单加购 / 清空购物车 / 写记事本备忘（仅匹配或写入，不下单）
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiCartController {

    private final DeepSeekService deepSeekService;
    private final NotebookService notebookService;

    /**
     * POST /api/ai/cart  {"text":"来份清淡的蛋羹" | "清空购物车" | "记一下明天买花"}
     * 需登录 Session（ORDERER / CHEF 均可）
     * data.action: add_dishes | clear_cart | create_note | search_dishes
     */
    @PostMapping("/cart")
    @OperationLogAnnotation(
            module = "AI点单",
            operationType = "一句话助手",
            description = "DeepSeek 一句话：点单/搜索/清空购物车/写备忘",
            recordParams = true,
            recordResult = false
    )
    public Map<String, Object> oneSentenceCart(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String text = body == null || body.get("text") == null ? "" : String.valueOf(body.get("text")).trim();
        try {
            Map<String, Object> matched = deepSeekService.matchAndPrepareCart(text);
            boolean ok = Boolean.TRUE.equals(matched.get("ok"));
            int code = matched.get("code") instanceof Number ? ((Number) matched.get("code")).intValue() : (ok ? 200 : 500);
            result.put("code", code);
            result.put("message", matched.get("message"));
            if (ok) {
                String action = matched.get("action") == null ? "add_dishes" : String.valueOf(matched.get("action"));
                Map<String, Object> data = new HashMap<>();
                data.put("action", action);
                if ("clear_cart".equals(action)) {
                    // 购物车在前端 localStorage；此处只返回意图
                    result.put("data", data);
                    return result;
                }
                if ("search_dishes".equals(action)) {
                    data.put("keyword", matched.get("keyword"));
                    result.put("data", data);
                    return result;
                }
                if ("create_note".equals(action)) {
                    String title = matched.get("title") == null ? "" : String.valueOf(matched.get("title")).trim();
                    String content = matched.get("content") == null ? "" : String.valueOf(matched.get("content")).trim();
                    if (!StringUtils.hasText(content)) {
                        content = text;
                    }
                    if (!StringUtils.hasText(title)) {
                        title = content.length() > 16 ? content.substring(0, 16) : content;
                    }
                    Long uid = AuthService.forceSessionUserId(session, null);
                    Long familyId = AuthService.requireFamilyId(session);
                    Notebook note = new Notebook();
                    note.setUserId(uid);
                    note.setFamilyId(familyId);
                    note.setTitle(title);
                    note.setContent(content);
                    note.setCategory("备忘");
                    note.setIsPinned(false);
                    notebookService.addNote(note);
                    data.put("note", note);
                    result.put("message", "已写入记事本备忘：「" + title + "」📝");
                    result.put("data", data);
                    return result;
                }
                // add_dishes (default)
                data.put("added", matched.get("added"));
                data.put("candidates", matched.get("candidates"));
                data.put("autoAdded", matched.get("autoAdded"));
                result.put("data", data);
            }
            return result;
        } catch (Exception e) {
            log.error("一句话助手失败", e);
            result.put("code", 500);
            result.put("message", "一句话助手失败：" + e.getMessage());
            return result;
        }
    }
}
