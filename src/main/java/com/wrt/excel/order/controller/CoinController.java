package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.CoinTransaction;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.CoinService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 爱心币控制器（自助路径强制 Session；加币仅 CHEF，可指定目标用户）
 */
@Slf4j
@RestController
@RequestMapping("/api/coin")
public class CoinController {
    
    @Autowired
    private CoinService coinService;
    
    @GetMapping("/balance/{userId}")
    public Map<String, Object> getBalance(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            Integer balance = coinService.getBalance(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", balance);
        } catch (Exception e) {
            log.error("获取余额失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 当前登录用户余额（推荐前端使用） */
    @GetMapping("/balance")
    public Map<String, Object> getMyBalance(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.requireSessionUserId(session);
            Integer balance = coinService.getBalance(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", balance);
        } catch (Exception e) {
            log.error("获取余额失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/add")
    public Map<String, Object> addCoins(@RequestBody Map<String, Object> params, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long clientUid = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
            // CHEF 可给他人加币；ORDERER 不可达此接口（拦截器）
            Long userId = AuthService.resolveTargetUserId(session, clientUid);
            Integer amount = Integer.valueOf(params.get("amount").toString());
            Object descObj = params.get("description") != null ? params.get("description") : params.get("remark");
            String description = descObj != null ? descObj.toString() : null;
            String type = params.get("type") != null ? params.get("type").toString() : "奖励";
            
            coinService.addCoins(userId, amount, description, type);
            
            Integer newBalance = coinService.getBalance(userId);
            result.put("code", 200);
            result.put("message", "爱心币增加成功 💕");
            result.put("data", newBalance);
        } catch (Exception e) {
            log.error("增加爱心币失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/use")
    public Map<String, Object> useCoins(@RequestBody Map<String, Object> params, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long clientUid = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
            Long userId = AuthService.resolveTargetUserId(session, clientUid);
            Integer amount = Integer.valueOf(params.get("amount").toString());
            String type = params.get("type") != null ? params.get("type").toString() : "CONSUME";
            Object descObjUse = params.get("description") != null ? params.get("description") : params.get("remark");
            String description = descObjUse != null ? descObjUse.toString() : type;
            Long relatedId = params.get("relatedId") != null ? Long.valueOf(params.get("relatedId").toString()) : null;
            
            coinService.useCoins(userId, amount, type, description, relatedId);
            
            Integer newBalance = coinService.getBalance(userId);
            result.put("code", 200);
            result.put("message", "爱心币使用成功 💕");
            result.put("data", newBalance);
        } catch (Exception e) {
            log.error("使用爱心币失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/transactions/{userId}")
    public Map<String, Object> getTransactions(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            List<CoinTransaction> transactions = coinService.getTransactions(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", transactions);
        } catch (Exception e) {
            log.error("获取流水记录失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
