package com.wrt.excel.order.controller;

import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.LotteryService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 转盘抽奖控制器（强制 Session 用户）
 */
@Slf4j
@RestController
@RequestMapping("/api/lottery")
public class LotteryController {
    
    @Autowired
    private LotteryService lotteryService;
    
    @PostMapping("/draw")
    public Map<String, Object> draw(@RequestParam(required = false) Long userId, HttpSession session) {
        Long uid = AuthService.forceSessionUserId(session, userId);
        log.info("用户 {} 请求抽奖", uid);
        return lotteryService.drawLottery(uid);
    }
    
    @GetMapping("/prizes")
    public Map<String, Object> getPrizes() {
        Map<String, Object> result = new HashMap<>();
        try {
            var prizes = lotteryService.getAllPrizes();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", prizes);
        } catch (Exception e) {
            log.error("获取奖品列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/history/{userId}")
    public Map<String, Object> getHistory(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            var history = lotteryService.getUserLotteryHistory(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", history);
        } catch (Exception e) {
            log.error("获取抽奖历史失败", e);
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
            var stats = lotteryService.getUserLotteryStats(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取抽奖统计失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/stats/global")
    public Map<String, Object> getGlobalStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            var stats = lotteryService.getGlobalLotteryStats();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取全局抽奖统计失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
