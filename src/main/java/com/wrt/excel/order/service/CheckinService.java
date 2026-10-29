package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.DailyCheckin;
import com.wrt.excel.order.mapper.DailyCheckinMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 每日签到服务
 */
@Slf4j
@Service
public class CheckinService {
    
    @Autowired
    private DailyCheckinMapper dailyCheckinMapper;
    
    @Autowired
    private CoinService coinService;
    
    /**
     * 每日签到
     * @param userId 用户ID
     * @return 签到结果信息
     */
    @Transactional
    public Map<String, Object> checkin(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        
        // 检查今天是否已经签到
        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCheckin::getUserId, userId)
               .eq(DailyCheckin::getCheckinDate, today);
        
        DailyCheckin todayCheckin = dailyCheckinMapper.selectOne(wrapper);
        if (todayCheckin != null) {
            result.put("code", 200);
            result.put("success", false);
            result.put("message", "今天已经签到过了哦~ 💕");
            result.put("alreadyCheckedIn", true);
            return result;
        }
        
        // 计算连续签到天数
        int continuousDays = calculateContinuousDays(userId, today);
        
        // 计算奖励爱心币（连续签到越多，奖励越多）
        int coinsReward = calculateCoinsReward(continuousDays);
        
        // 创建签到记录
        DailyCheckin checkin = new DailyCheckin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(today);
        checkin.setContinuousDays(continuousDays);
        checkin.setCoinsReward(coinsReward);
        
        dailyCheckinMapper.insert(checkin);
        
        // 增加爱心币
        coinService.addCoins(userId, coinsReward, "每日签到奖励（连续" + continuousDays + "天）", "signin");
        
        log.info("用户 {} 签到成功，连续签到 {} 天，获得 {} 爱心币", userId, continuousDays, coinsReward);
        
        result.put("code", 200);
        result.put("success", true);
        result.put("message", "签到成功！获得 " + coinsReward + " 爱心币 💕");
        result.put("continuousDays", continuousDays);
        result.put("coinsReward", coinsReward);
            result.put("reward", coinsReward);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("reward", coinsReward);
            data.put("coins", coinsReward);
            data.put("amount", coinsReward);
            data.put("continuousDays", continuousDays);
            result.put("data", data);
        result.put("alreadyCheckedIn", false);
        
        return result;
    }
    
    /**
     * 计算连续签到天数
     * @param userId 用户ID
     * @param today 今天日期
     * @return 连续签到天数
     */
    private int calculateContinuousDays(Long userId, LocalDate today) {
        // 查询昨天的签到记录
        LocalDate yesterday = today.minusDays(1);
        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCheckin::getUserId, userId)
               .eq(DailyCheckin::getCheckinDate, yesterday);
        
        DailyCheckin yesterdayCheckin = dailyCheckinMapper.selectOne(wrapper);
        
        if (yesterdayCheckin != null) {
            // 如果昨天签到了，连续天数+1
            Integer prev = yesterdayCheckin.getContinuousDays();
            return (prev != null ? prev : 0) + 1;
        } else {
            // 如果昨天没签到，从1开始
            return 1;
        }
    }
    
    /**
     * 根据连续签到天数计算奖励
     * @param continuousDays 连续签到天数
     * @return 奖励爱心币数量
     */
    private int calculateCoinsReward(int continuousDays) {
        if (continuousDays >= 30) {
            return 20; // 连续30天以上，奖励20个
        } else if (continuousDays >= 14) {
            return 15; // 连续14天以上，奖励15个
        } else if (continuousDays >= 7) {
            return 10; // 连续7天以上，奖励10个
        } else if (continuousDays >= 3) {
            return 7;  // 连续3天以上，奖励7个
        } else {
            return 5;  // 基础奖励5个
        }
    }
    
    /**
     * 检查今天是否已签到
     * @param userId 用户ID
     * @return 是否已签到
     */
    public boolean isCheckedInToday(Long userId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCheckin::getUserId, userId)
               .eq(DailyCheckin::getCheckinDate, today);
        
        return dailyCheckinMapper.selectCount(wrapper) > 0;
    }
    
    /**
     * 获取用户的签到统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    public Map<String, Object> getCheckinStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 总签到天数
        LambdaQueryWrapper<DailyCheckin> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(DailyCheckin::getUserId, userId);
        long totalDays = dailyCheckinMapper.selectCount(totalWrapper);
        
        // 本月签到天数
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LambdaQueryWrapper<DailyCheckin> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(DailyCheckin::getUserId, userId)
                    .ge(DailyCheckin::getCheckinDate, firstDayOfMonth);
        long monthDays = dailyCheckinMapper.selectCount(monthWrapper);
        
        // 当前连续签到天数：仅当最近一次签到是今天或昨天时，连击仍有效
        LambdaQueryWrapper<DailyCheckin> continuousWrapper = new LambdaQueryWrapper<>();
        continuousWrapper.eq(DailyCheckin::getUserId, userId)
                         .orderByDesc(DailyCheckin::getCheckinDate)
                         .last("LIMIT 1");
        DailyCheckin latestCheckin = dailyCheckinMapper.selectOne(continuousWrapper);
        
        int currentContinuousDays = 0;
        if (latestCheckin != null && latestCheckin.getCheckinDate() != null) {
            LocalDate latestDate = latestCheckin.getCheckinDate();
            LocalDate today = LocalDate.now();
            if (latestDate.equals(today) || latestDate.equals(today.minusDays(1))) {
                Integer cd = latestCheckin.getContinuousDays();
                currentContinuousDays = cd != null ? cd : 0;
            }
        }
        
        stats.put("totalDays", totalDays);
        stats.put("monthDays", monthDays);
        stats.put("currentContinuousDays", currentContinuousDays);
        // 兼容前端字段名
        stats.put("continuousDays", currentContinuousDays);
        stats.put("streak", currentContinuousDays);
        stats.put("continuous", currentContinuousDays);
        stats.put("isCheckedInToday", isCheckedInToday(userId));
        
        return stats;
    }
    
    /**
     * 获取用户的签到历史记录
     * @param userId 用户ID
     * @return 签到历史列表
     */
    public List<DailyCheckin> getCheckinHistory(Long userId) {
        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCheckin::getUserId, userId)
               .orderByDesc(DailyCheckin::getCheckinDate)
               .last("LIMIT 30"); // 最近30条
        
        return dailyCheckinMapper.selectList(wrapper);
    }
    
    /**
     * 获取全局签到统计（用于后台管理）
     * @return 全局统计信息
     */
    public Map<String, Object> getGlobalCheckinStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总签到次数
        long totalCheckins = dailyCheckinMapper.selectCount(null);
        
        // 今日签到人数
        LambdaQueryWrapper<DailyCheckin> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(DailyCheckin::getCheckinDate, LocalDate.now());
        long todayCheckins = dailyCheckinMapper.selectCount(todayWrapper);
        
        // 本月签到次数
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LambdaQueryWrapper<DailyCheckin> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.ge(DailyCheckin::getCheckinDate, firstDayOfMonth);
        long monthCheckins = dailyCheckinMapper.selectCount(monthWrapper);
        
        // 参与签到的用户数
        LambdaQueryWrapper<DailyCheckin> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.select(DailyCheckin::getUserId)
                   .groupBy(DailyCheckin::getUserId);
        long uniqueUsers = dailyCheckinMapper.selectCount(userWrapper);
        
        // 发放的爱心币总数
        List<DailyCheckin> allCheckins = dailyCheckinMapper.selectList(null);
        int totalCoinsReward = allCheckins.stream()
                .mapToInt(DailyCheckin::getCoinsReward)
                .sum();
        
        // 最高连续签到天数
        LambdaQueryWrapper<DailyCheckin> maxWrapper = new LambdaQueryWrapper<>();
        maxWrapper.orderByDesc(DailyCheckin::getContinuousDays)
                  .last("LIMIT 1");
        DailyCheckin maxCheckin = dailyCheckinMapper.selectOne(maxWrapper);
        int maxContinuousDays = maxCheckin != null ? maxCheckin.getContinuousDays() : 0;
        
        stats.put("totalCheckins", totalCheckins);
        stats.put("todayCheckins", todayCheckins);
        stats.put("monthCheckins", monthCheckins);
        stats.put("uniqueUsers", uniqueUsers);
        stats.put("totalCoinsReward", totalCoinsReward);
        stats.put("maxContinuousDays", maxContinuousDays);
        
        return stats;
    }
}
