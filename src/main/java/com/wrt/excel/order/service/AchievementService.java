package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.Achievement;
import com.wrt.excel.order.entity.CoinBalance;
import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.entity.Order;
import com.wrt.excel.order.entity.OrderItem;
import com.wrt.excel.order.entity.UserAchievement;
import com.wrt.excel.order.mapper.AchievementMapper;
import com.wrt.excel.order.mapper.CoinBalanceMapper;
import com.wrt.excel.order.mapper.DishMapper;
import com.wrt.excel.order.mapper.OrderItemMapper;
import com.wrt.excel.order.mapper.OrderMapper;
import com.wrt.excel.order.mapper.UserAchievementMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成就服务类
 */
@Slf4j
@Service
public class AchievementService {
    
    @Autowired
    private AchievementMapper achievementMapper;
    
    @Autowired
    private UserAchievementMapper userAchievementMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private CoinBalanceMapper coinBalanceMapper;
    
    @Autowired
    private CoinService coinService;
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private OrderItemMapper orderItemMapper;
    
    /**
     * 检查并解锁成就（在下单后调用）
     */
    @Transactional
    public List<Achievement> checkAndUnlockAchievements(Long userId) {
        log.info("开始检查用户 {} 的成就解锁条件", userId);
        
        List<Achievement> unlockedAchievements = new ArrayList<>();
        
        // 获取所有启用的成就
        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Achievement::getIsActive, 1);
        List<Achievement> allAchievements = achievementMapper.selectList(wrapper);
        
        for (Achievement achievement : allAchievements) {
            // 检查是否已解锁
            if (isAchievementUnlocked(userId, achievement.getId())) {
                continue;
            }
            
            // 检查是否满足条件
            boolean isUnlocked = false;
            switch (achievement.getConditionType()) {
                case "order_count":
                    isUnlocked = checkOrderCount(userId, achievement.getConditionValue());
                    break;
                case "consecutive_days":
                    isUnlocked = checkConsecutiveDays(userId, achievement.getConditionValue());
                    break;
                case "try_all_dishes":
                    isUnlocked = checkTryAllDishes(userId);
                    break;
                case "special_dish":
                    isUnlocked = checkSpecialDish(userId);
                    break;
                default:
                    log.warn("未知的成就条件类型: {}", achievement.getConditionType());
            }
            
            if (isUnlocked) {
                // 解锁成就
                unlockAchievement(userId, achievement.getId());
                unlockedAchievements.add(achievement);
                
                // 发放奖励
                grantReward(userId, achievement);
                
                log.info("用户 {} 解锁成就: {}", userId, achievement.getName());
            }
        }
        
        return unlockedAchievements;
    }
    
    /**
     * 检查订单数量成就
     */
    private boolean checkOrderCount(Long userId, int requiredCount) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .eq(Order::getStatus, "completed");
        Long count = orderMapper.selectCount(wrapper);
        return count >= requiredCount;
    }
    
    /**
     * 检查连续点单天数成就
     */
    private boolean checkConsecutiveDays(Long userId, int requiredDays) {
        // 获取用户所有已完成订单的日期
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .eq(Order::getStatus, "completed")
               .orderByDesc(Order::getCreatedAt);
        List<Order> orders = orderMapper.selectList(wrapper);
        
        if (orders.isEmpty()) {
            return false;
        }
        
        // 计算连续天数
        TreeSet<LocalDate> uniqueDates = orders.stream()
                .map(order -> order.getCreatedAt().toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));
        
        if (uniqueDates.isEmpty()) {
            return false;
        }
        
        // 从最近一天开始往前计算连续天数
        LocalDate today = LocalDate.now();
        LocalDate lastDate = uniqueDates.last();
        
        // 如果最后一天不是今天或昨天，则连续天数为0
        if (lastDate.isBefore(today.minusDays(1))) {
            return false;
        }
        
        int consecutiveDays = 1;
        LocalDate currentDate = lastDate;
        
        while (currentDate.isAfter(today.minusDays(requiredDays))) {
            currentDate = currentDate.minusDays(1);
            if (uniqueDates.contains(currentDate)) {
                consecutiveDays++;
            } else {
                break;
            }
        }
        
        return consecutiveDays >= requiredDays;
    }
    
    /**
     * 检查是否尝试所有菜品成就
     */
    private boolean checkTryAllDishes(Long userId) {
        // 获取所有菜品ID
        LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
        dishWrapper.eq(Dish::getIsSoldOut, 0);
        List<Dish> allDishes = dishMapper.selectList(dishWrapper);
        
        if (allDishes.isEmpty()) {
            return false;
        }
        
        Set<Long> allDishIds = allDishes.stream()
                .map(Dish::getId)
                .collect(Collectors.toSet());
        
        // 获取用户已点过的菜品ID
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getUserId, userId)
                    .eq(Order::getStatus, "completed");
        List<Order> completedOrders = orderMapper.selectList(orderWrapper);
        
        Set<Long> orderedDishIds = new HashSet<>();
        for (Order order : completedOrders) {
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
            
            items.forEach(item -> orderedDishIds.add(item.getDishId()));
        }
        
        // 检查是否包含所有菜品
        return orderedDishIds.containsAll(allDishIds);
    }
    
    /**
     * 检查特殊菜品成就（例如点了所有拿手菜）
     */
    private boolean checkSpecialDish(Long userId) {
        // 这里可以实现特殊的菜品检查逻辑
        // 暂时返回false，可根据需求扩展
        return false;
    }
    
    /**
     * 检查成就是否已解锁
     */
    private boolean isAchievementUnlocked(Long userId, Long achievementId) {
        LambdaQueryWrapper<UserAchievement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAchievement::getUserId, userId)
               .eq(UserAchievement::getAchievementId, achievementId)
               .eq(UserAchievement::getIsUnlocked, 1);
        return userAchievementMapper.selectCount(wrapper) > 0;
    }
    
    /**
     * 解锁成就
     */
    private void unlockAchievement(Long userId, Long achievementId) {
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUserId(userId);
        userAchievement.setAchievementId(achievementId);
        userAchievement.setUnlockedAt(LocalDateTime.now());
        userAchievement.setIsUnlocked(1);
        userAchievementMapper.insert(userAchievement);
    }
    
    /**
     * 发放成就奖励
     */
    private void grantReward(Long userId, Achievement achievement) {
        // 如果成就有爱心币奖励，则发放
        if (achievement.getRewardCoins() != null && achievement.getRewardCoins() > 0) {
            coinService.addCoins(userId, achievement.getRewardCoins(), 
                "成就奖励: " + achievement.getName(), "achievement");
            log.info("用户 {} 获得成就奖励: {} 爱心币", userId, achievement.getRewardCoins());
        }
    }
    
    /**
     * 获取用户已解锁的成就列表
     */
    public List<Map<String, Object>> getUserUnlockedAchievements(Long userId) {
        LambdaQueryWrapper<UserAchievement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAchievement::getUserId, userId)
               .eq(UserAchievement::getIsUnlocked, 1)
               .orderByDesc(UserAchievement::getUnlockedAt);
        List<UserAchievement> userAchievements = userAchievementMapper.selectList(wrapper);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserAchievement ua : userAchievements) {
            Achievement achievement = achievementMapper.selectById(ua.getAchievementId());
            if (achievement != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", achievement.getId());
                map.put("name", achievement.getName());
                map.put("description", achievement.getDescription());
                map.put("icon", achievement.getIcon());
                map.put("reward", achievement.getReward());
                map.put("rewardCoins", achievement.getRewardCoins());
                map.put("unlockedAt", ua.getUnlockedAt());
                result.add(map);
            }
        }
        
        return result;
    }
    
    /**
     * 获取所有成就及解锁状态
     */
    public List<Map<String, Object>> getAllAchievementsWithStatus(Long userId) {
        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Achievement::getIsActive, 1);
        List<Achievement> allAchievements = achievementMapper.selectList(wrapper);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Achievement achievement : allAchievements) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", achievement.getId());
            map.put("name", achievement.getName());
            map.put("description", achievement.getDescription());
            map.put("icon", achievement.getIcon());
            map.put("conditionType", achievement.getConditionType());
            map.put("conditionValue", achievement.getConditionValue());
            map.put("reward", achievement.getReward());
            map.put("rewardCoins", achievement.getRewardCoins());
            map.put("isUnlocked", isAchievementUnlocked(userId, achievement.getId()));
            
            // 如果已解锁，添加解锁时间
            if (map.get("isUnlocked").equals(true)) {
                LambdaQueryWrapper<UserAchievement> uaWrapper = new LambdaQueryWrapper<>();
                uaWrapper.eq(UserAchievement::getUserId, userId)
                         .eq(UserAchievement::getAchievementId, achievement.getId());
                UserAchievement ua = userAchievementMapper.selectOne(uaWrapper);
                if (ua != null) {
                    map.put("unlockedAt", ua.getUnlockedAt());
                }
            }
            
            result.add(map);
        }
        
        return result;
    }
    
    /**
     * 获取用户的成就统计信息
     */
    public Map<String, Object> getAchievementStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 总成就数
        LambdaQueryWrapper<Achievement> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(Achievement::getIsActive, 1);
        long totalAchievements = achievementMapper.selectCount(allWrapper);
        
        // 已解锁成就数
        LambdaQueryWrapper<UserAchievement> unlockedWrapper = new LambdaQueryWrapper<>();
        unlockedWrapper.eq(UserAchievement::getUserId, userId)
                       .eq(UserAchievement::getIsUnlocked, 1);
        long unlockedCount = userAchievementMapper.selectCount(unlockedWrapper);
        
        // 完成度
        double completionRate = totalAchievements > 0 ? 
            (double) unlockedCount / totalAchievements * 100 : 0;
        
        stats.put("totalAchievements", totalAchievements);
        stats.put("total", totalAchievements);
        stats.put("totalCount", totalAchievements);
        stats.put("unlockedCount", unlockedCount);
        stats.put("unlocked", unlockedCount);
        stats.put("completionRate", String.format("%.1f", completionRate));
        
        return stats;
    }
    
    /**
     * 获取全局成就统计（用于后台管理）
     * @return 全局统计信息
     */
    public Map<String, Object> getGlobalAchievementStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总成就数
        LambdaQueryWrapper<Achievement> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(Achievement::getIsActive, 1);
        long totalAchievements = achievementMapper.selectCount(allWrapper);
        
        // 总解锁次数
        LambdaQueryWrapper<UserAchievement> unlockedWrapper = new LambdaQueryWrapper<>();
        unlockedWrapper.eq(UserAchievement::getIsUnlocked, 1);
        long totalUnlocks = userAchievementMapper.selectCount(unlockedWrapper);
        
        // 参与成就的用户数
        LambdaQueryWrapper<UserAchievement> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.select(UserAchievement::getUserId)
                   .groupBy(UserAchievement::getUserId);
        long uniqueUsers = userAchievementMapper.selectCount(userWrapper);
        
        // 今日解锁成就数
        LambdaQueryWrapper<UserAchievement> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(UserAchievement::getIsUnlocked, 1)
                    .ge(UserAchievement::getUnlockedAt, LocalDateTime.now().toLocalDate().atStartOfDay());
        long todayUnlocks = userAchievementMapper.selectCount(todayWrapper);
        
        // 发放的爱心币奖励总数
        List<UserAchievement> allUnlocks = userAchievementMapper.selectList(unlockedWrapper);
        int totalRewardCoins = 0;
        for (UserAchievement ua : allUnlocks) {
            Achievement achievement = achievementMapper.selectById(ua.getAchievementId());
            if (achievement != null && achievement.getRewardCoins() != null) {
                totalRewardCoins += achievement.getRewardCoins();
            }
        }
        
        stats.put("totalAchievements", totalAchievements);
        stats.put("totalUnlocks", totalUnlocks);
        stats.put("uniqueUsers", uniqueUsers);
        stats.put("todayUnlocks", todayUnlocks);
        stats.put("totalRewardCoins", totalRewardCoins);
        
        return stats;
    }
}
