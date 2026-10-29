package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.BlindBoxRecord;
import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.mapper.BlindBoxRecordMapper;
import com.wrt.excel.order.mapper.DishMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 盲盒服务
 */
@Slf4j
@Service
public class BlindBoxService {
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private DislikeService dislikeService;
    
    @Autowired
    private CoinService coinService;

    @Autowired
    private BlindBoxRecordMapper blindBoxRecordMapper;
    
    /**
     * 盲盒抽菜（排除不喜欢的菜品）
     */
    public Dish drawBlindBox(Long userId) {
        List<Long> dislikeDishIds = dislikeService.getUserDislikeDishIds(userId);
        
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getIsSoldOut, 0);
        List<Dish> allDishes = dishMapper.selectList(wrapper);
        
        if (allDishes.isEmpty()) {
            log.warn("没有在售菜品");
            return null;
        }
        
        List<Dish> availableDishes = allDishes.stream()
                .filter(dish -> !dislikeDishIds.contains(dish.getId()))
                .collect(Collectors.toList());
        
        if (availableDishes.isEmpty()) {
            log.warn("用户 {} 排除了所有菜品，无法进行盲盒抽取", userId);
            return null;
        }
        
        int randomIndex = (int) (Math.random() * availableDishes.size());
        Dish selectedDish = availableDishes.get(randomIndex);
        
        log.info("用户 {} 盲盒抽取到菜品: {}", userId, selectedDish.getName());
        
        return selectedDish;
    }

    /**
     * 保存抽取记录
     */
    public BlindBoxRecord saveRecord(Long userId, Dish dish, Integer cost) {
        BlindBoxRecord record = new BlindBoxRecord();
        record.setUserId(userId);
        record.setDishId(dish.getId());
        record.setDishName(dish.getName());
        record.setDishPrice(dish.getPrice());
        record.setCostCoins(cost != null ? cost : 0);
        record.setCreatedAt(LocalDateTime.now());
        blindBoxRecordMapper.insert(record);
        return record;
    }

    /**
     * 用户盲盒历史（新→旧）
     */
    public List<BlindBoxRecord> getHistory(Long userId, int limit) {
        int lim = limit <= 0 ? 20 : Math.min(limit, 100);
        return blindBoxRecordMapper.selectList(new LambdaQueryWrapper<BlindBoxRecord>()
                .eq(BlindBoxRecord::getUserId, userId)
                .orderByDesc(BlindBoxRecord::getCreatedAt)
                .last("LIMIT " + lim));
    }
    
    /** 服务端固定盲盒费用，忽略客户端传入的 cost，防止改包白嫖 */
    public static final int BLIND_BOX_COST = 30;

    /**
     * 扣币 + 抽菜 + 记流水；任一步失败整单回滚（含退回已扣币）
     */
    @Transactional(rollbackFor = Exception.class)
    public java.util.Map<String, Object> drawAndCharge(Long userId) {
        coinService.useCoins(userId, BLIND_BOX_COST, "use_blind_box", "盲盒点单费用", null);
        Dish dish = drawBlindBox(userId);
        if (dish == null) {
            throw new IllegalArgumentException("没有可用的菜品进行抽取");
        }
        BlindBoxRecord record = saveRecord(userId, dish, BLIND_BOX_COST);
        java.util.Map<String, Object> out = new java.util.HashMap<>();
        out.put("dish", dish);
        out.put("record", record);
        return out;
    }

    /** @deprecated 请用 drawAndCharge；保留仅兼容旧调用 */
    public boolean tryDeductCost(Long userId, Integer cost) {
        try {
            int realCost = BLIND_BOX_COST;
            Integer balance = coinService.getBalance(userId);
            if (balance < realCost) {
                log.warn("用户 {} 爱心币余额不足: {} < {}", userId, balance, realCost);
                return false;
            }
            coinService.useCoins(userId, realCost, "use_blind_box", "盲盒点单费用", null);
            log.info("用户 {} 扣除盲盒费用 {} 爱心币", userId, realCost);
            return true;
        } catch (Exception e) {
            log.error("扣除盲盒费用失败", e);
            return false;
        }
    }
}
