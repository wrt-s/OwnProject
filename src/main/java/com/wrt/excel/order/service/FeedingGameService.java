package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.FeedingGameRecord;
import com.wrt.excel.order.mapper.FeedingGameRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 喂食大作战游戏服务
 */
@Slf4j
@Service
public class FeedingGameService {
    
    @Autowired
    private FeedingGameRecordMapper feedingGameRecordMapper;
    
    /**
     * 保存游戏记录
     * @param record 游戏记录
     */
    public void saveGameRecord(FeedingGameRecord record) {
        feedingGameRecordMapper.insert(record);
        log.info("用户 {} 完成喂食大作战游戏，得分: {}", record.getUserId(), record.getScore());
    }
    
    /**
     * 获取用户的游戏历史记录
     * @param userId 用户ID
     * @return 游戏记录列表
     */
    public List<FeedingGameRecord> getUserGameHistory(Long userId, int limit) {
        LambdaQueryWrapper<FeedingGameRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedingGameRecord::getUserId, userId)
               .orderByDesc(FeedingGameRecord::getCreatedAt)
               .last("LIMIT " + limit);
        return feedingGameRecordMapper.selectList(wrapper);
    }
    
    /**
     * 获取用户的最高分
     * @param userId 用户ID
     * @return 最高分记录
     */
    public FeedingGameRecord getUserBestScore(Long userId) {
        LambdaQueryWrapper<FeedingGameRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedingGameRecord::getUserId, userId)
               .orderByDesc(FeedingGameRecord::getScore)
               .last("LIMIT 1");
        return feedingGameRecordMapper.selectOne(wrapper);
    }
    
    /**
     * 获取用户的统计数据
     * @param userId 用户ID
     * @return 统计数据
     */
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 总游戏次数
        LambdaQueryWrapper<FeedingGameRecord> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(FeedingGameRecord::getUserId, userId);
        Long totalGames = feedingGameRecordMapper.selectCount(totalWrapper);
        stats.put("totalGames", totalGames);
        
        // 最高分
        FeedingGameRecord bestRecord = getUserBestScore(userId);
        if (bestRecord != null) {
            stats.put("bestScore", bestRecord.getScore());
            stats.put("bestEffect", bestRecord.getUnlockedEffect());
        } else {
            stats.put("bestScore", 0);
            stats.put("bestEffect", "无");
        }
        
        // 平均得分
        if (totalGames > 0) {
            LambdaQueryWrapper<FeedingGameRecord> scoreWrapper = new LambdaQueryWrapper<>();
            scoreWrapper.eq(FeedingGameRecord::getUserId, userId)
                       .select(FeedingGameRecord::getScore);
            List<FeedingGameRecord> records = feedingGameRecordMapper.selectList(scoreWrapper);
            double avgScore = records.stream()
                    .mapToInt(FeedingGameRecord::getScore)
                    .average()
                    .orElse(0.0);
            stats.put("avgScore", Math.round(avgScore * 100.0) / 100.0);
        } else {
            stats.put("avgScore", 0.0);
        }
        
        return stats;
    }
    
    /**
     * 根据分数计算解锁的特殊效果
     * @param score 分数
     * @return 特殊效果描述
     */
    public String getUnlockedEffect(int score) {
        if (score >= 500) {
            return "🌟 传说级喂食大师 - 解锁彩虹特效";
        } else if (score >= 400) {
            return "✨ 史诗级喂食者 - 解锁金色光晕";
        } else if (score >= 300) {
            return "💫 钻石级美食家 - 解锁星光闪烁";
        } else if (score >= 200) {
            return "⭐ 黄金级吃货 - 解锁爱心飘浮";
        } else if (score >= 100) {
            return "🎯 白银级品尝者 - 解锁彩色背景";
        } else {
            return "🎮 新手玩家 - 继续努力";
        }
    }
}
