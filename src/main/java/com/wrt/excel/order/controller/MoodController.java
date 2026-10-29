package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.MoodLog;
import com.wrt.excel.order.entity.User;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.MoodService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 心情记录 API：家庭共享；每人每日一条
 */
@Slf4j
@RestController
@RequestMapping("/api/mood")
public class MoodController {

    @Autowired
    private MoodService moodService;

    @Autowired
    private AuthService authService;

    private void assertFamilyAccess(MoodLog item, HttpSession session) {
        if (item == null) {
            throw new IllegalArgumentException("心情记录不存在");
        }
        AuthService.assertSameFamily(item.getFamilyId(), session);
    }

    /**
     * 今日：自己 + 对方（按家庭成员排除自己）
     */
    @GetMapping("/today")
    public Map<String, Object> today(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            Long familyId = AuthService.requireFamilyId(session);
            MoodLog self = moodService.getUserMoodOnDate(uid, moodService.today());
            User partner = moodService.findPartner(familyId, uid);
            MoodLog partnerMood = null;
            String partnerNickname = null;
            Long partnerUserId = null;
            if (partner != null) {
                partnerUserId = partner.getId();
                partnerNickname = partner.getNickname() != null ? partner.getNickname() : "对方";
                partnerMood = moodService.getUserMoodOnDate(partnerUserId, moodService.today());
            }
            Map<String, Object> data = new HashMap<>();
            data.put("date", moodService.today().toString());
            data.put("self", self);
            data.put("partner", partnerMood);
            data.put("partnerUserId", partnerUserId);
            data.put("partnerNickname", partnerNickname != null ? partnerNickname : "对方");
            data.put("moods", MoodService.MOOD_CATALOG);
            data.put("selfUserId", uid);
            result.put("code", 200);
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取今日心情失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 家庭最近心情列表
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false, defaultValue = "30") Integer limit,
                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            Long uid = AuthService.requireSessionUserId(session);
            List<MoodLog> list = moodService.listFamilyRecent(familyId, limit == null ? 30 : limit);
            result.put("code", 200);
            result.put("data", list);
            result.put("count", list.size());
            result.put("selfUserId", uid);
            result.put("nicknames", moodService.nicknameMap(familyId));
            result.put("moods", MoodService.MOOD_CATALOG);
        } catch (Exception e) {
            log.error("获取心情列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 保存/更新今日心情（upsert）
     * body: { mood, note? } — 忽略任何 userId / partnerUserId，仅写当前 Session 用户今日行
     */
    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody Map<String, Object> body, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 强制 Session 用户；客户端 userId / partnerUserId 一律忽略，防止覆盖对方
            if (body != null && (body.containsKey("userId") || body.containsKey("partnerUserId")
                    || body.containsKey("user_id") || body.containsKey("targetUserId"))) {
                log.warn("mood/save ignored client userId fields from session user");
            }
            Long uid = AuthService.requireSessionUserId(session);
            Long familyId = AuthService.requireFamilyId(session);
            String mood = body == null || body.get("mood") == null ? "" : String.valueOf(body.get("mood")).trim();
            String note = body != null && body.get("note") != null ? String.valueOf(body.get("note")) : null;
            MoodLog saved = moodService.upsertToday(uid, familyId, mood, note);
            result.put("code", 200);
            result.put("message", "已记录今日心情 " + MoodService.emojiOf(mood));
            result.put("data", saved);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("保存心情失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            AuthService.requireSessionUserId(session);
            MoodLog existing = moodService.getById(id);
            assertFamilyAccess(existing, session);
            // 仅允许删自己的记录
            Long uid = AuthService.requireSessionUserId(session);
            if (!uid.equals(existing.getUserId())) {
                result.put("code", 403);
                result.put("message", "只能删除自己的心情记录");
                return result;
            }
            moodService.delete(id);
            result.put("code", 200);
            result.put("message", "已删除");
        } catch (Exception e) {
            log.error("删除心情失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }


    /**
     * 近 7 天周历（LEFT=点单方 RIGHT=做餐方）+ 简单趋势。
     * 只读家庭范围；保存仍走 /save（仅自己）。
     */
    @GetMapping("/week")
    public Map<String, Object> week(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            Long uid = AuthService.requireSessionUserId(session);
            Map<String, Object> data = moodService.weekOverview(familyId);
            data.put("selfUserId", uid);
            result.put("code", 200);
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取心情周历失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 按日期区间列出家庭心情（from/to=yyyy-MM-dd，默认近 7 天）。
     */
    @GetMapping("/range")
    public Map<String, Object> range(@RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to,
                                     HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            Long uid = AuthService.requireSessionUserId(session);
            LocalDate end = to != null && !to.isBlank() ? LocalDate.parse(to.trim()) : moodService.today();
            LocalDate start = from != null && !from.isBlank() ? LocalDate.parse(from.trim()) : end.minusDays(6);
            List<MoodLog> list = moodService.listFamilyRange(familyId, start, end);
            result.put("code", 200);
            result.put("data", list);
            result.put("count", list.size());
            result.put("from", start.toString());
            result.put("to", end.toString());
            result.put("selfUserId", uid);
            result.put("nicknames", moodService.nicknameMap(familyId));
            result.put("moods", MoodService.MOOD_CATALOG);
            // 角色锚点，便于前端按点单方/做餐方分列
            User orderer = moodService.findByRole(familyId, "ORDERER");
            User chef = moodService.findByRole(familyId, "CHEF");
            result.put("ordererUserId", orderer != null ? orderer.getId() : null);
            result.put("chefUserId", chef != null ? chef.getId() : null);
        } catch (IllegalArgumentException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("获取心情区间列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", MoodService.MOOD_CATALOG);
        return result;
    }
}
