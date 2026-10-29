package com.wrt.excel.order.controller;

import com.wrt.excel.order.entity.FeedingGameRecord;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.FeedingGameService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 喂食大作战游戏控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/feeding-game")
public class FeedingGameController {
    
    @Autowired
    private FeedingGameService feedingGameService;
    
    /**
     * 保存游戏记录
     * @param gameData 游戏数据
     * @return 保存结果
     */
    @PostMapping("/save")
    public Map<String, Object> saveGame(@RequestBody Map<String, Object> gameData, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long clientUid = gameData.get("userId") != null ? Long.valueOf(gameData.get("userId").toString()) : null;
            Long userId = AuthService.forceSessionUserId(session, clientUid);
            Integer score = Integer.valueOf(gameData.get("score").toString());
            Integer duration = Integer.valueOf(gameData.get("duration").toString());
            // FE may post eaten/score/duration; API stores foodCount (+ optional darkFoodCount)
            Object foodRaw = gameData.get("foodCount") != null ? gameData.get("foodCount") : gameData.get("eaten");
            if (foodRaw == null) {
                result.put("code", 400);
                result.put("message", "缺少 foodCount/eaten");
                return result;
            }
            Integer foodCount = Integer.valueOf(foodRaw.toString());
            Object darkRaw = gameData.get("darkFoodCount");
            Integer darkFoodCount = darkRaw != null ? Integer.valueOf(darkRaw.toString()) : 0;
            
            // 创建游戏记录
            FeedingGameRecord record = new FeedingGameRecord();
            record.setUserId(userId);
            record.setScore(score);
            record.setDuration(duration);
            record.setFoodCount(foodCount);
            record.setDarkFoodCount(darkFoodCount);
            
            // 计算解锁的特殊效果
            String unlockedEffect = feedingGameService.getUnlockedEffect(score);
            record.setUnlockedEffect(unlockedEffect);
            
            // 保存记录
            feedingGameService.saveGameRecord(record);
            
            result.put("code", 200);
            result.put("message", "游戏记录保存成功");
            result.put("data", record);
        } catch (Exception e) {
            log.error("保存游戏记录失败", e);
            result.put("code", 500);
            result.put("message", "保存失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取用户的游戏历史
     * @param userId 用户ID
     * @return 游戏历史记录
     */
    @GetMapping("/history/{userId}")
    public Map<String, Object> getHistory(@PathVariable Long userId, 
                                          @RequestParam(defaultValue = "10") int limit,
                                          HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            List<FeedingGameRecord> history = feedingGameService.getUserGameHistory(uid, limit);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", history);
        } catch (Exception e) {
            log.error("获取游戏历史失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取用户的最高分
     * @param userId 用户ID
     * @return 最高分记录
     */
    @GetMapping("/best-score/{userId}")
    public Map<String, Object> getBestScore(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            FeedingGameRecord bestRecord = feedingGameService.getUserBestScore(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", bestRecord);
        } catch (Exception e) {
            log.error("获取最高分失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取用户的统计数据
     * @param userId 用户ID
     * @return 统计数据
     */
    @GetMapping("/stats/{userId}")
    public Map<String, Object> getStats(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            Map<String, Object> stats = feedingGameService.getUserStats(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
