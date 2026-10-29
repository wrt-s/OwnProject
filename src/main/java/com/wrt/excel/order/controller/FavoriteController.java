package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.FavoriteService;
import com.wrt.excel.system.service.FileUploadService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏控制器（强制 Session 用户）
 */
@Slf4j
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {
    
    @Autowired
    private FavoriteService favoriteService;

    @Autowired(required = false)
    private FileUploadService fileUploadService;
    
    @PostMapping("/add")
    public Map<String, Object> addFavorite(@RequestBody Map<String, Long> params, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = AuthService.forceSessionUserId(session, params.get("userId"));
            Long familyId = AuthService.requireFamilyId(session);
            Long dishId = params.get("dishId");
            favoriteService.addFavorite(userId, familyId, dishId);
            result.put("code", 200);
            result.put("message", "收藏成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("添加收藏失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/remove")
    public Map<String, Object> removeFavorite(@RequestBody Map<String, Long> params, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = AuthService.forceSessionUserId(session, params.get("userId"));
            Long familyId = AuthService.requireFamilyId(session);
            Long dishId = params.get("dishId");
            favoriteService.removeFavorite(userId, familyId, dishId);
            result.put("code", 200);
            result.put("message", "取消收藏成功");
        } catch (Exception e) {
            log.error("取消收藏失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/list/{userId}")
    public Map<String, Object> getUserFavorites(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            List<Dish> favorites = favoriteService.getUserFavorites(familyId);
            if (favorites != null && fileUploadService != null) {
                for (Dish dish : favorites) {
                    if (dish != null) {
                        dish.setImageUrl(fileUploadService.toReadableUrl(dish.getImageUrl()));
                    }
                }
            }
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", favorites);
        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/check/{userId}/{dishId}")
    public Map<String, Object> checkFavorite(@PathVariable Long userId, @PathVariable Long dishId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long familyId = AuthService.requireFamilyId(session);
            boolean favorited = favoriteService.isFavorited(familyId, dishId);
            result.put("code", 200);
            result.put("data", favorited);
        } catch (Exception e) {
            log.error("检查收藏状态失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/stats/global")
    public Map<String, Object> getGlobalFavoriteStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            var stats = favoriteService.getGlobalFavoriteStats();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取全局收藏统计失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
