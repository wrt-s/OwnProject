package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.entity.DishFavorite;
import com.wrt.excel.order.mapper.DishFavoriteMapper;
import com.wrt.excel.order.mapper.DishMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收藏服务
 */
@Slf4j
@Service
public class FavoriteService {
    
    @Autowired
    private DishFavoriteMapper dishFavoriteMapper;
    
    @Autowired
    private DishMapper dishMapper;
    
    /**
     * 添加收藏
     */
    @Transactional
    public void addFavorite(Long userId, Long familyId, Long dishId) {
        if (familyId == null) familyId = 1L;
        LambdaQueryWrapper<DishFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishFavorite::getFamilyId, familyId)
               .eq(DishFavorite::getDishId, dishId);
        if (dishFavoriteMapper.selectCount(wrapper) > 0) {
            throw new IllegalStateException("已经收藏过该菜品");
        }
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new IllegalArgumentException("菜品不存在");
        }
        DishFavorite favorite = new DishFavorite();
        favorite.setUserId(userId);
        favorite.setFamilyId(familyId);
        favorite.setDishId(dishId);
        dishFavoriteMapper.insert(favorite);
        log.info("家庭 {} 用户 {} 收藏菜品 {}", familyId, userId, dishId);
    }
    
    /**
     * 取消收藏
     */
    @Transactional
    public void removeFavorite(Long userId, Long familyId, Long dishId) {
        if (familyId == null) familyId = 1L;
        LambdaQueryWrapper<DishFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishFavorite::getFamilyId, familyId)
               .eq(DishFavorite::getDishId, dishId);
        dishFavoriteMapper.delete(wrapper);
        log.info("家庭 {} 用户 {} 取消收藏菜品 {}", familyId, userId, dishId);
    }
    
    /**
     * 获取用户收藏列表（包含菜品信息）
     */
    public List<Dish> getUserFavorites(Long familyId) {
        if (familyId == null) familyId = 1L;
        LambdaQueryWrapper<DishFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishFavorite::getFamilyId, familyId)
               .orderByDesc(DishFavorite::getCreatedAt);
        List<DishFavorite> favorites = dishFavoriteMapper.selectList(wrapper);
        List<Long> dishIds = favorites.stream()
                .map(DishFavorite::getDishId)
                .collect(Collectors.toList());
        if (dishIds.isEmpty()) {
            return List.of();
        }
        return dishMapper.selectBatchIds(dishIds);
    }
    
    /**
     * 检查是否已收藏
     */
    public boolean isFavorited(Long familyId, Long dishId) {
        if (familyId == null) familyId = 1L;
        LambdaQueryWrapper<DishFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishFavorite::getFamilyId, familyId)
               .eq(DishFavorite::getDishId, dishId);
        return dishFavoriteMapper.selectCount(wrapper) > 0;
    }
    
    /**
     * 获取全局收藏统计（用于后台管理）
     * @return 全局统计信息
     */
    public Map<String, Object> getGlobalFavoriteStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总收藏次数
        long totalFavorites = dishFavoriteMapper.selectCount(null);
        
        // 参与收藏的用户数
        LambdaQueryWrapper<DishFavorite> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.select(DishFavorite::getUserId)
                   .groupBy(DishFavorite::getUserId);
        long uniqueUsers = dishFavoriteMapper.selectCount(userWrapper);
        
        // 被收藏的菜品数
        LambdaQueryWrapper<DishFavorite> dishWrapper = new LambdaQueryWrapper<>();
        dishWrapper.select(DishFavorite::getDishId)
                   .groupBy(DishFavorite::getDishId);
        long uniqueDishes = dishFavoriteMapper.selectCount(dishWrapper);
        
        // 今日新增收藏
        LambdaQueryWrapper<DishFavorite> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(DishFavorite::getCreatedAt, java.time.LocalDate.now().atStartOfDay());
        long todayFavorites = dishFavoriteMapper.selectCount(todayWrapper);
        
        stats.put("totalFavorites", totalFavorites);
        stats.put("uniqueUsers", uniqueUsers);
        stats.put("uniqueDishes", uniqueDishes);
        stats.put("todayFavorites", todayFavorites);
        
        return stats;
    }
}
