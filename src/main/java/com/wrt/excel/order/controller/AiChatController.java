package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.Anniversary;
import com.wrt.excel.order.entity.MoodLog;
import com.wrt.excel.order.entity.Notebook;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.service.AnniversaryService;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.CoinService;
import com.wrt.excel.order.service.DeepSeekService;
import com.wrt.excel.order.service.MoodService;
import com.wrt.excel.order.service.NotebookService;
import com.wrt.excel.system.interfance.OperationLogAnnotation;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 小布 AI 对话：闲聊 + 点单/备忘/爱心币/心情/纪念日工具（必须真实读库/写库后才能宣称成功）。
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiChatController {

    private static final String SESSION_HISTORY = "XIAOBU_CHAT_HISTORY";
    private static final int MAX_HISTORY = 12; // 6 轮

    private final DeepSeekService deepSeekService;
    private final NotebookService notebookService;
    private final CoinService coinService;
    private final MoodService moodService;
    private final AnniversaryService anniversaryService;
    private final AuthService authService;

    /**
     * POST /api/ai/chat
     * body: { "message": "...", "history": optional [{role,content}] }
     */
    @PostMapping("/chat")
    @OperationLogAnnotation(
            module = "AI小布",
            operationType = "对话",
            description = "小布对话",
            recordParams = false,
            recordResult = false
    )
    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            AuthService.requireSessionUserId(session);
            String message = body == null || body.get("message") == null
                    ? "" : String.valueOf(body.get("message")).trim();
            if (!StringUtils.hasText(message)) {
                result.put("code", 400);
                result.put("message", "先跟小布说点什么呀。");
                return result;
            }
            if (message.length() > 500) {
                message = message.substring(0, 500);
            }

            List<Map<String, String>> history = new ArrayList<>();
            Object clientHist = body.get("history");
            if (clientHist instanceof List<?> list && !list.isEmpty()) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    Object role = m.get("role");
                    Object content = m.get("content");
                    if (role == null || content == null) continue;
                    String r = String.valueOf(role);
                    String c = String.valueOf(content);
                    if (("user".equals(r) || "assistant".equals(r)) && StringUtils.hasText(c)) {
                        history.add(Map.of("role", r, "content", c.length() > 800 ? c.substring(0, 800) : c));
                    }
                }
            } else {
                Object sess = session.getAttribute(SESSION_HISTORY);
                if (sess instanceof List<?> sl) {
                    for (Object o : sl) {
                        if (o instanceof Map<?, ?> m) {
                            Object role = m.get("role");
                            Object content = m.get("content");
                            if (role != null && content != null) {
                                history.add(Map.of("role", String.valueOf(role), "content", String.valueOf(content)));
                            }
                        }
                    }
                }
            }
            if (history.size() > MAX_HISTORY) {
                history = new ArrayList<>(history.subList(history.size() - MAX_HISTORY, history.size()));
            }

            Map<String, Object> chatOut = deepSeekService.chatXiaobu(message, history);
            boolean ok = Boolean.TRUE.equals(chatOut.get("ok"));
            int code = chatOut.get("code") instanceof Number
                    ? ((Number) chatOut.get("code")).intValue() : (ok ? 200 : 500);
            result.put("code", code);
            result.put("message", chatOut.get("message"));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("assistant", "小布");
            data.put("reply", chatOut.get("reply"));

            String action = chatOut.get("action") == null ? null : String.valueOf(chatOut.get("action"));
            if (ok && StringUtils.hasText(action) && !"chat".equals(action)) {
                data.put("action", action);
                if ("create_note".equals(action)) {
                    String title = chatOut.get("title") == null ? "" : String.valueOf(chatOut.get("title")).trim();
                    String content = chatOut.get("content") == null ? "" : String.valueOf(chatOut.get("content")).trim();
                    if (!StringUtils.hasText(content)) content = message;
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
                    if (!StringUtils.hasText(String.valueOf(data.get("reply")))) {
                        data.put("reply", "记下啦，已经放进记事本里了：「" + title + "」～");
                    }
                } else if ("clear_cart".equals(action)) {
                    // 前端清 localStorage；同步清 session，避免返回点单页又被 /api/cart 拉回
                    CartController.clear(session);
                } else if ("search_dishes".equals(action)) {
                    data.put("keyword", chatOut.get("keyword"));
                } else if ("list_dishes".equals(action) || "recommend_from_menu".equals(action)) {
                    data.put("dishes", chatOut.get("dishes"));
                    data.put("suggested", chatOut.get("suggested"));
                } else if ("add_dishes".equals(action)) {
                    Object addedObj = chatOut.get("added");
                    List<?> addedList = addedObj instanceof List<?> ? (List<?>) addedObj : List.of();
                    data.put("candidates", chatOut.get("candidates"));
                    data.put("autoAdded", chatOut.get("autoAdded"));
                    // 成功话术只能在 session 购物车确实写入后出现
                    if (addedList.isEmpty()) {
                        data.put("action", "chat");
                        data.put("added", List.of());
                        data.put("cartSynced", false);
                        data.put("reply", "呜…菜单里没有对上的菜，我没加进购物车。去「点单」挑现有的，或告诉我菜单上的菜名～");
                    } else {
                        int merged = 0;
                        try {
                            merged = CartController.mergeAdded(session, addedList);
                        } catch (Exception mergeEx) {
                            log.warn("session cart merge failed", mergeEx);
                            merged = 0;
                        }
                        List<Map<String, Object>> serverCart = CartController.snapshot(session);
                        boolean durable = merged > 0 && serverCartContainsAll(serverCart, addedList);
                        if (durable) {
                            data.put("action", "add_dishes");
                            data.put("added", addedObj);
                            data.put("cartSynced", true);
                            data.put("serverCart", serverCart);
                            data.put("reply", buildAddCartSuccessReply(addedList));
                        } else {
                            data.put("action", "chat");
                            data.put("added", List.of());
                            data.put("cartSynced", false);
                            data.put("reply", "呜…我这边没能把菜写进购物车，刚才不能算加成功。去「点单」页看看，或再说一次菜名，我再帮你加～");
                        }
                    }
                } else if ("query_coins".equals(action)) {
                    applyQueryCoins(session, data);
                } else if ("save_mood".equals(action)) {
                    applySaveMood(session, chatOut, data);
                } else if ("anni_countdown".equals(action)) {
                    applyAnniCountdown(session, chatOut, data);
                }
            } else {
                data.put("action", "chat");
            }


            // 防御：仅当本次走过加购并明确 cartSynced=false 时，才剥掉成功话术
            // （不可用 !cartSynced：recommend 等未设置该字段时会被误伤，把「要不要加进购物车」改成失败话）
            if (data.containsKey("cartSynced") && !Boolean.TRUE.equals(data.get("cartSynced"))
                    && looksLikeAddSuccessClaim(String.valueOf(data.get("reply")))) {
                data.put("reply", "呜…我还没能确认购物车写入成功，先别当真。去「点单」页看一眼，或再说一次要加的菜～");
                data.put("action", "chat");
            }
            // 成功话术与 top-level message 对齐，避免 FE 用 r.message 回落到 DeepSeek 未确认文案
            if (Boolean.TRUE.equals(data.get("cartSynced")) && "add_dishes".equals(String.valueOf(data.get("action")))) {
                result.put("message", data.get("reply"));
            } else if (data.containsKey("cartSynced") && !Boolean.TRUE.equals(data.get("cartSynced"))) {
                result.put("message", data.get("reply"));
            }

            // 更新 session 对话历史
            List<Map<String, String>> next = new ArrayList<>(history);
            next.add(Map.of("role", "user", "content", message));
            String reply = data.get("reply") == null ? "" : String.valueOf(data.get("reply"));
            if (StringUtils.hasText(reply)) {
                next.add(Map.of("role", "assistant", "content", reply));
            }
            if (next.size() > MAX_HISTORY) {
                next = new ArrayList<>(next.subList(next.size() - MAX_HISTORY, next.size()));
            }
            session.setAttribute(SESSION_HISTORY, next);
            data.put("historySize", next.size());

            result.put("data", data);
            if (ok && result.get("message") == null) {
                result.put("message", "ok");
            }
            return result;
        } catch (Exception e) {
            log.error("小布对话失败", e);
            result.put("code", 500);
            result.put("message", "小布刚才走神了，待会再试试。");
            return result;
        }
    }

    /**
     * 查询点单方爱心币余额（家庭下 ORDERER）；读库后才回报。
     */
    private void applyQueryCoins(HttpSession session, Map<String, Object> data) {
        Long familyId = AuthService.requireFamilyId(session);
        User orderer = authService.findFamilyOrderer(familyId);
        Long coinUid = orderer != null && orderer.getId() != null
                ? orderer.getId()
                : AuthService.requireSessionUserId(session);
        Integer balance = coinService.getBalance(coinUid);
        if (balance == null) balance = 0;
        data.put("balance", balance);
        data.put("coinUserId", coinUid);
        data.put("coinScope", "orderer");
        data.put("reply", "查过啦～点单方现在有 " + balance + " 爱心币。要花的话去点单页下单就好～");
    }

    /**
     * 仅写入当前登录用户今日心情（self only）。
     */
    private void applySaveMood(HttpSession session, Map<String, Object> chatOut, Map<String, Object> data) {
        Long uid = AuthService.requireSessionUserId(session);
        Long familyId = AuthService.requireFamilyId(session);
        String mood = chatOut.get("mood") == null ? "" : String.valueOf(chatOut.get("mood")).trim();
        try {
            MoodLog saved = moodService.upsertToday(uid, familyId, mood, null);
            data.put("mood", saved);
            data.put("moodCode", mood);
            data.put("emoji", MoodService.emojiOf(mood));
            data.put("reply", "记下啦，今天的心情是「" + mood + "」" + MoodService.emojiOf(mood)
                    + "（只改你自己的，不会动到对方）。回首页就能看到心情双点更新～");
        } catch (IllegalArgumentException ex) {
            data.put("action", "chat");
            data.put("reply", "心情要选这几个之一哦：开心、平静、想你、累了、低落、生气、想吃、无聊、焦虑、睡意。你再说一次？");
        }
    }

    private void applyAnniCountdown(HttpSession session, Map<String, Object> chatOut, Map<String, Object> data) {
        Long familyId = AuthService.requireFamilyId(session);
        String keyword = chatOut.get("keyword") == null ? "" : String.valueOf(chatOut.get("keyword")).trim();
        List<Anniversary> all = anniversaryService.getUserAnniversaries(familyId);
        if (all == null) all = List.of();

        List<Map<String, Object>> rows = new ArrayList<>();
        Anniversary matched = null;
        Anniversary nearest = null;
        int nearestDays = Integer.MAX_VALUE;
        for (Anniversary a : all) {
            if (a == null || a.getDate() == null) continue;
            int days = anniversaryService.getDaysUntilAnniversary(a);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("name", a.getName());
            row.put("date", a.getDate().toString());
            row.put("daysLeft", days);
            rows.add(row);
            if (StringUtils.hasText(keyword) && a.getName() != null && a.getName().contains(keyword)) {
                if (matched == null || days < anniversaryService.getDaysUntilAnniversary(matched)) {
                    matched = a;
                }
            }
            if (days < nearestDays) {
                nearestDays = days;
                nearest = a;
            }
        }
        rows.sort(Comparator.comparingInt(r -> ((Number) r.get("daysLeft")).intValue()));
        data.put("anniversaries", rows.size() > 5 ? rows.subList(0, 5) : rows);

        Anniversary pick = matched != null ? matched : nearest;
        if (pick == null) {
            data.put("reply", "家里还没有纪念日呢。你可以在纪念日页先加一个，我再帮你倒数～");
            return;
        }
        int days = anniversaryService.getDaysUntilAnniversary(pick);
        data.put("name", pick.getName());
        data.put("date", pick.getDate() == null ? null : pick.getDate().toString());
        data.put("daysLeft", days);
        if (days == 0) {
            data.put("reply", "就是今天！「" + pick.getName() + "」到啦，要不要搞点小仪式感～");
        } else {
            data.put("reply", "「" + pick.getName() + "」还有 " + days + " 天哦"
                    + (matched == null && rows.size() > 1 ? "（最近的一个）" : "") + "～");
        }
    }


    private static boolean looksLikeAddSuccessClaim(String reply) {
        if (reply == null || reply.isEmpty()) return false;
        // 否定 / 征求确认：不算成功宣称（避免误伤推荐话术「要不要加进购物车」）
        if (reply.contains("没加") || reply.contains("没有加") || reply.contains("未能")
                || reply.contains("要不要") || reply.contains("能不能") || reply.contains("先别当真")
                || reply.contains("还没能")) {
            return false;
        }
        return reply.contains("已加入") || reply.contains("加好了") || reply.contains("已经加进")
                || reply.contains("已为你匹配并加入") || reply.contains("已经加进购物车")
                || reply.contains("帮你加进购物车了") || reply.contains("加进购物车啦")
                || reply.contains("加进购物车了");
    }

    private static String buildAddCartSuccessReply(List<?> addedList) {
        StringBuilder names = new StringBuilder();
        int n = 0;
        for (Object o : addedList) {
            if (!(o instanceof Map<?, ?> m)) continue;
            Object name = m.get("dishName") != null ? m.get("dishName") : m.get("name");
            Object qty = m.get("quantity") != null ? m.get("quantity") : m.get("qty");
            if (name == null) continue;
            if (n > 0) names.append("、");
            names.append(name).append("×").append(qty == null ? "1" : String.valueOf(qty));
            n++;
        }
        if (n == 0) {
            return "好～已经加进购物车啦，去点单页确认一下呀～";
        }
        return "好～已为你匹配并加入：" + names + " 请到购物车确认呀～";
    }

    @SuppressWarnings("unchecked")
    private static boolean serverCartContainsAll(List<Map<String, Object>> serverCart, List<?> addedList) {
        if (serverCart == null || serverCart.isEmpty() || addedList == null || addedList.isEmpty()) {
            return false;
        }
        java.util.Set<Long> have = new java.util.HashSet<>();
        for (Map<String, Object> row : serverCart) {
            Object id = row.get("dishId");
            if (id instanceof Number) have.add(((Number) id).longValue());
        }
        int need = 0;
        int ok = 0;
        for (Object o : addedList) {
            if (!(o instanceof Map<?, ?> m)) continue;
            Object raw = m.get("dishId") != null ? m.get("dishId") : m.get("id");
            if (raw == null) continue;
            long id;
            try { id = Long.parseLong(String.valueOf(raw)); } catch (Exception e) { continue; }
            if (id <= 0) continue;
            need++;
            if (have.contains(id)) ok++;
        }
        return need > 0 && ok == need;
    }

    /** 清空服务端会话历史 */
    @PostMapping("/chat/reset")
    public Map<String, Object> reset(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            AuthService.requireSessionUserId(session);
            session.removeAttribute(SESSION_HISTORY);
            result.put("code", 200);
            result.put("message", "已开启新对话");
            result.put("data", Map.of("assistant", "小布"));
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
