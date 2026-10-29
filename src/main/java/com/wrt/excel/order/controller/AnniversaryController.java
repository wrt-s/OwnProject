package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.Anniversary;
import com.wrt.excel.order.service.AnniversaryService;
import com.wrt.excel.order.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 纪念日控制器（强制 Session 归属）
 */
@Slf4j
@RestController
@RequestMapping("/api/anniversary")
public class AnniversaryController {
    
    @Autowired
    private AnniversaryService anniversaryService;

    private void assertFamilyAccess(Anniversary item, HttpSession session) {
        if (item == null) {
            throw new IllegalArgumentException("纪念日不存在");
        }
        AuthService.assertSameFamily(item.getFamilyId(), session);
    }
    
    @PostMapping("/add")
    public Map<String, Object> addAnniversary(@RequestBody Anniversary anniversary, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.forceSessionUserId(session, anniversary.getUserId());
            Long familyId = AuthService.requireFamilyId(session);
            anniversary.setUserId(uid);
            anniversary.setFamilyId(familyId);
            anniversaryService.addAnniversary(anniversary);
            result.put("code", 200);
            result.put("message", "添加纪念日成功 💕");
        } catch (Exception e) {
            log.error("添加纪念日失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/update")
    public Map<String, Object> updateAnniversary(@RequestBody Anniversary anniversary, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            Anniversary existing = anniversaryService.getById(anniversary.getId());
            assertFamilyAccess(existing, session);
            anniversary.setUserId(existing.getUserId());
            anniversary.setFamilyId(existing.getFamilyId());
            anniversaryService.updateAnniversary(anniversary);
            result.put("code", 200);
            result.put("message", "更新纪念日成功");
        } catch (Exception e) {
            log.error("更新纪念日失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteAnniversary(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            assertFamilyAccess(anniversaryService.getById(id), session);
            anniversaryService.deleteAnniversary(id);
            result.put("code", 200);
            result.put("message", "删除纪念日成功");
        } catch (Exception e) {
            log.error("删除纪念日失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/list/{userId}")
    public Map<String, Object> getUserAnniversaries(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            List<Anniversary> anniversaries = anniversaryService.getUserAnniversaries(familyId);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", anniversaries);
        } catch (Exception e) {
            log.error("获取纪念日列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/upcoming/{userId}")
    public Map<String, Object> getUpcomingAnniversaries(@PathVariable Long userId,
                                                         @RequestParam(defaultValue = "30") int days,
                                                         HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            List<Anniversary> upcoming = anniversaryService.getUpcomingAnniversaries(familyId, days);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", upcoming);
        } catch (Exception e) {
            log.error("获取即将到来纪念日失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
