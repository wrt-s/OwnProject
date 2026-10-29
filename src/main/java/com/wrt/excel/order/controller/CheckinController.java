package com.wrt.excel.order.controller;

import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.CheckinService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 每日签到控制器（强制 Session 用户）
 */
@Slf4j
@RestController
@RequestMapping("/api/checkin")
public class CheckinController {
    
    @Autowired
    private CheckinService checkinService;
    
    @PostMapping("/do")
    public Map<String, Object> doCheckin(@RequestParam(required = false) Long userId, HttpSession session) {
        Long uid = AuthService.forceSessionUserId(session, userId);
        log.info("用户 {} 请求签到", uid);
        return checkinService.checkin(uid);
    }
    
    @GetMapping("/status/{userId}")
    public Map<String, Object> getCheckinStatus(@PathVariable Long userId, HttpSession session) {
        Long uid = AuthService.resolveTargetUserId(session, userId);
        boolean isCheckedIn = checkinService.isCheckedInToday(uid);
        Map<String, Object> result = new java.util.HashMap<>();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("checkedIn", isCheckedIn);
        data.put("hasCheckedIn", isCheckedIn);
        data.put("todayChecked", isCheckedIn);
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }

    @GetMapping("/status")
    public Map<String, Object> getMyCheckinStatus(HttpSession session) {
        Long uid = AuthService.requireSessionUserId(session);
        boolean isCheckedIn = checkinService.isCheckedInToday(uid);
        Map<String, Object> result = new java.util.HashMap<>();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("checkedIn", isCheckedIn);
        data.put("hasCheckedIn", isCheckedIn);
        data.put("todayChecked", isCheckedIn);
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
    
    @GetMapping("/stats/{userId}")
    public Map<String, Object> getCheckinStats(@PathVariable Long userId, HttpSession session) {
        Long uid = AuthService.resolveTargetUserId(session, userId);
        Map<String, Object> stats = checkinService.getCheckinStats(uid);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("code", 200);
        result.put("data", stats);
        return result;
    }
    
    @GetMapping("/stats/global")
    public Map<String, Object> getGlobalCheckinStats() {
        Map<String, Object> stats = checkinService.getGlobalCheckinStats();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", stats);
        return result;
    }
    
    @GetMapping("/history/{userId}")
    public Map<String, Object> getCheckinHistory(@PathVariable Long userId, HttpSession session) {
        Long uid = AuthService.resolveTargetUserId(session, userId);
        var history = checkinService.getCheckinHistory(uid);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("code", 200);
        result.put("data", history);
        return result;
    }
}
