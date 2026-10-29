package com.wrt.excel.order.controller;

import com.wrt.excel.order.service.AchievementService;
import com.wrt.excel.order.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成就控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/achievement")
public class AchievementController {
    
    @Autowired
    private AchievementService achievementService;
    
    /**
     * 获取用户已解锁的成就列表
     */
    @GetMapping("/unlocked/{userId}")
    public Map<String, Object> getUnlockedAchievements(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            List<Map<String, Object>> achievements = achievementService.getUserUnlockedAchievements(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", achievements);
        } catch (Exception e) {
            log.error("获取已解锁成就失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取所有成就及解锁状态
     */
    @GetMapping("/list/{userId}")
    public Map<String, Object> getAllAchievements(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            List<Map<String, Object>> achievements = achievementService.getAllAchievementsWithStatus(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", achievements);
        } catch (Exception e) {
            log.error("获取成就列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取成就统计信息
     */
    @GetMapping("/stats/{userId}")
    public Map<String, Object> getAchievementStats(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            Map<String, Object> stats = achievementService.getAchievementStats(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取成就统计失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取全局成就统计（用于后台管理）
     * @return 全局统计信息
     */
    @GetMapping("/stats/global")
    public Map<String, Object> getGlobalAchievementStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = achievementService.getGlobalAchievementStats();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取全局成就统计失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 手动触发成就检查（用于测试）
     */
    @PostMapping("/check/{userId}")
    public Map<String, Object> checkAchievements(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.forceSessionUserId(session, userId);
            List<?> unlockedAchievements = achievementService.checkAndUnlockAchievements(uid);
            result.put("code", 200);
            result.put("message", "成就检查完成");
            result.put("data", unlockedAchievements);
        } catch (Exception e) {
            log.error("成就检查失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
