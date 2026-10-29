package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.entity.DishDislike;
import com.wrt.excel.order.mapper.DishDislikeMapper;
import com.wrt.excel.order.mapper.DishMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 不喜欢菜品服务
 */
@Slf4j
@Service
public class DislikeService {
    
    @Autowired
    private DishDislikeMapper dishDislikeMapper;
    
    @Autowired
    private DishMapper dishMapper;
    
    /**
     * 添加不喜欢的菜品
     */
    @Transactional
    public void addDislike(Long userId, Long dishId) {
        // 检查是否已添加
        LambdaQueryWrapper<DishDislike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishDislike::getUserId, userId)
               .eq(DishDislike::getDishId, dishId);
        
        if (dishDislikeMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("已经添加过该菜品到不喜欢列表");
        }
        
        // 检查菜品是否存在
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }
        
        DishDislike dislike = new DishDislike();
        dislike.setUserId(userId);
        dislike.setDishId(dishId);
        dishDislikeMapper.insert(dislike);
        
        log.info("用户 {} 添加不喜欢菜品 {}", userId, dishId);
    }
    
    /**
     * 移除不喜欢的菜品
     */
    @Transactional
    public void removeDislike(Long userId, Long dishId) {
        LambdaQueryWrapper<DishDislike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishDislike::getUserId, userId)
               .eq(DishDislike::getDishId, dishId);
        
        dishDislikeMapper.delete(wrapper);
        
        log.info("用户 {} 移除不喜欢菜品 {}", userId, dishId);
    }
    
    /**
     * 获取用户不喜欢的菜品ID列表
     */
    public List<Long> getUserDislikeDishIds(Long userId) {
        LambdaQueryWrapper<DishDislike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishDislike::getUserId, userId);
        
        List<DishDislike> dislikes = dishDislikeMapper.selectList(wrapper);
        return dislikes.stream()
                .map(DishDislike::getDishId)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查菜品是否在不喜欢列表中
     */
    public boolean isDisliked(Long userId, Long dishId) {
        LambdaQueryWrapper<DishDislike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishDislike::getUserId, userId)
               .eq(DishDislike::getDishId, dishId);
        
        return dishDislikeMapper.selectCount(wrapper) > 0;
    }
}
