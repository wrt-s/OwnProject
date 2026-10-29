package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.BlindBoxRecord;
import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.BlindBoxService;
import com.wrt.excel.order.service.DislikeService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 盲盒控制器（强制 Session 用户 + 服务端历史）
 */
@Slf4j
@RestController
@RequestMapping("/api/blindbox")
public class BlindBoxController {
    
    @Autowired
    private BlindBoxService blindBoxService;
    
    @Autowired
    private DislikeService dislikeService;
    
        @PostMapping("/draw")
    public Map<String, Object> draw(@RequestBody(required = false) Map<String, Object> params, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (params == null) {
                params = new HashMap<>();
            }
            Long clientUid = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
            Long userId = AuthService.forceSessionUserId(session, clientUid);
            // 费用由服务端固定，忽略客户端 cost
            int cost = BlindBoxService.BLIND_BOX_COST;

            java.util.Map<String, Object> drawn = blindBoxService.drawAndCharge(userId);
            Dish dish = (Dish) drawn.get("dish");
            BlindBoxRecord record = (BlindBoxRecord) drawn.get("record");

            result.put("code", 200);
            result.put("message", "盲盒抽取成功！");
            result.put("data", dish);
            result.put("cost", cost);
            result.put("recordId", record.getId());
        } catch (IllegalArgumentException e) {
            log.warn("盲盒抽取业务失败: {}", e.getMessage());
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("盲盒抽取失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 盲盒历史：默认当前 Session 用户；CHEF 可传 userId 查看他人
     */
    @GetMapping("/history")
    public Map<String, Object> history(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "20") int limit,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            List<BlindBoxRecord> list = blindBoxService.getHistory(uid, limit);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", list);
        } catch (Exception e) {
            log.error("获取盲盒历史失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/history/{userId}")
    public Map<String, Object> historyByPath(@PathVariable Long userId,
                                             @RequestParam(defaultValue = "20") int limit,
                                             HttpSession session) {
        return history(userId, limit, session);
    }
    
    @PostMapping("/dislike/add")
    public Map<String, Object> addDislike(@RequestBody Map<String, Long> params, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = AuthService.forceSessionUserId(session, params.get("userId"));
            Long dishId = params.get("dishId");
            dislikeService.addDislike(userId, dishId);
            result.put("code", 200);
            result.put("message", "已添加到不喜欢列表 💔");
        } catch (Exception e) {
            log.error("添加不喜欢菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/dislike/remove")
    public Map<String, Object> removeDislike(@RequestBody Map<String, Long> params, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = AuthService.forceSessionUserId(session, params.get("userId"));
            Long dishId = params.get("dishId");
            dislikeService.removeDislike(userId, dishId);
            result.put("code", 200);
            result.put("message", "已从不喜欢的列表中移除");
        } catch (Exception e) {
            log.error("移除不喜欢菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/dislike/list/{userId}")
    public Map<String, Object> getDislikeList(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            List<Long> dislikeIds = dislikeService.getUserDislikeDishIds(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dislikeIds);
        } catch (Exception e) {
            log.error("获取不喜欢列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/dislike/check/{userId}/{dishId}")
    public Map<String, Object> checkDislike(@PathVariable Long userId, @PathVariable Long dishId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            boolean isDisliked = dislikeService.isDisliked(uid, dishId);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", isDisliked);
        } catch (Exception e) {
            log.error("检查不喜欢状态失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
