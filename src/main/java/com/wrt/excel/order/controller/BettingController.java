package com.wrt.excel.order.controller;

import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.BettingService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 幸运币押注控制器（强制 Session 用户）
 */
@Slf4j
@RestController
@RequestMapping("/api/betting")
public class BettingController {
    
    @Autowired
    private BettingService bettingService;
    
    @PostMapping("/place")
    public Map<String, Object> placeBet(@RequestBody Map<String, Object> params, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long clientUid = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
            Long userId = AuthService.forceSessionUserId(session, clientUid);
            Integer betAmount = Integer.valueOf(params.get("betAmount").toString());
            String betType = params.get("betType").toString();
            
            log.info("用户 {} 请求押注，金额：{}，类型：{}", userId, betAmount, betType);
            
            return bettingService.placeBet(userId, betAmount, betType);
        } catch (Exception e) {
            log.error("押注失败", e);
            result.put("success", false);
            result.put("message", "押注失败：" + e.getMessage());
            return result;
        }
    }
    
    @GetMapping("/history/{userId}")
    public Map<String, Object> getHistory(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            var history = bettingService.getUserBettingHistory(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", history);
        } catch (Exception e) {
            log.error("获取押注历史失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/stats/{userId}")
    public Map<String, Object> getStats(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            var stats = bettingService.getUserBettingStats(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取押注统计失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
