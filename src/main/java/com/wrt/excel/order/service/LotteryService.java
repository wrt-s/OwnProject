package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.LotteryPrize;
import com.wrt.excel.order.entity.LotteryRecord;
import com.wrt.excel.order.mapper.LotteryPrizeMapper;
import com.wrt.excel.order.mapper.LotteryRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 转盘抽奖服务
 */
@Slf4j
@Service
public class LotteryService {
    
    @Autowired
    private LotteryPrizeMapper lotteryPrizeMapper;
    
    @Autowired
    private LotteryRecordMapper lotteryRecordMapper;
    
    @Autowired
    private CoinService coinService;
    
    // 每次抽奖消耗的爱心币数量
    private static final int LOTTERY_COST = 10;
    
    /**
     * 执行抽奖
     * @param userId 用户ID
     * @return 抽奖结果
     */
    @Transactional
    public Map<String, Object> drawLottery(Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 检查余额是否充足
        Integer balance = coinService.getBalance(userId);
        if (balance < LOTTERY_COST) {
            result.put("success", false);
            result.put("message", "爱心币不足，需要" + LOTTERY_COST + "个爱心币才能抽奖哦~ 💕");
            result.put("balance", balance);
            return result;
        }
        
        // 2. 扣除爱心币
        try {
            coinService.useCoins(userId, LOTTERY_COST, "use_lottery", 
                "转盘抽奖消耗", null);
        } catch (Exception e) {
            log.error("扣除爱心币失败", e);
            result.put("success", false);
            result.put("message", "抽奖失败，请重试");
            return result;
        }
        
        // 3. 根据概率抽取奖品
        LotteryPrize prize = drawPrizeByProbability();
        
        if (prize == null) {
            // 抛出异常以回滚已扣除的爱心币
            throw new IllegalStateException("抽奖配置异常，请联系管理员");
        }
        
        // 4. 记录抽奖结果
        LotteryRecord record = new LotteryRecord();
        record.setUserId(userId);
        record.setPrizeId(prize.getId());
        record.setPrizeName(prize.getName());
        record.setPrizeType(prize.getPrizeType());
        record.setPrizeValue(prize.getValue());
        record.setCostCoins(LOTTERY_COST);
        
        lotteryRecordMapper.insert(record);
        
        // 5. 如果中奖的是爱心币，增加到用户余额
        String message;
        if ("coins".equals(prize.getPrizeType()) && prize.getValue() > 0) {
            coinService.addCoins(userId, prize.getValue(), 
                "转盘抽奖获得：" + prize.getName(), "lottery");
            message = "恭喜！抽中 " + prize.getName() + " 🎉";
        } else if ("thanks".equals(prize.getPrizeType())) {
            message = "谢谢惠顾，下次再来吧~ 😊";
        } else {
            message = "获得：" + prize.getName();
        }
        
        log.info("用户 {} 抽奖，消耗 {} 爱心币，获得: {}", userId, LOTTERY_COST, prize.getName());
        
        result.put("success", true);
        result.put("message", message);
        result.put("prize", prize);
        result.put("recordId", record.getId());
        result.put("newBalance", coinService.getBalance(userId));
        
        return result;
    }
    
    /**
     * 根据概率抽取奖品
     * @return 抽中的奖品
     */
    private LotteryPrize drawPrizeByProbability() {
        // 查询所有启用的奖品
        LambdaQueryWrapper<LotteryPrize> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LotteryPrize::getIsActive, 1)
               .orderByAsc(LotteryPrize::getSortOrder);
        
        List<LotteryPrize> prizes = lotteryPrizeMapper.selectList(wrapper);
        
        if (prizes.isEmpty()) {
            return null;
        }
        
        // 生成0-100的随机数
        double random = Math.random() * 100;
        
        // 累加概率，找到对应的奖品
        BigDecimal cumulativeProbability = BigDecimal.ZERO;
        
        for (LotteryPrize prize : prizes) {
            cumulativeProbability = cumulativeProbability.add(prize.getProbability());
            
            if (random < cumulativeProbability.doubleValue()) {
                return prize;
            }
        }
        
        // 理论上不会到这里，返回最后一个奖品
        return prizes.get(prizes.size() - 1);
    }
    
    /**
     * 获取所有奖品配置
     * @return 奖品列表
     */
    public List<LotteryPrize> getAllPrizes() {
        LambdaQueryWrapper<LotteryPrize> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LotteryPrize::getIsActive, 1)
               .orderByAsc(LotteryPrize::getSortOrder);
        
        return lotteryPrizeMapper.selectList(wrapper);
    }
    
    /**
     * 获取用户的抽奖历史记录
     * @param userId 用户ID
     * @return 抽奖记录列表
     */
    public List<LotteryRecord> getUserLotteryHistory(Long userId) {
        LambdaQueryWrapper<LotteryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LotteryRecord::getUserId, userId)
               .orderByDesc(LotteryRecord::getCreatedAt)
               .last("LIMIT 50"); // 最近50条
        
        return lotteryRecordMapper.selectList(wrapper);
    }
    
    /**
     * 获取用户的抽奖统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    public Map<String, Object> getUserLotteryStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 总抽奖次数
        LambdaQueryWrapper<LotteryRecord> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(LotteryRecord::getUserId, userId);
        long totalDraws = lotteryRecordMapper.selectCount(totalWrapper);
        
        // 总消耗爱心币
        long totalCost = totalDraws * LOTTERY_COST;
        
        // 总获得爱心币（只统计中奖的）
        LambdaQueryWrapper<LotteryRecord> winWrapper = new LambdaQueryWrapper<>();
        winWrapper.eq(LotteryRecord::getUserId, userId)
                  .eq(LotteryRecord::getPrizeType, "coins");
        
        List<LotteryRecord> winRecords = lotteryRecordMapper.selectList(winWrapper);
        int totalWin = winRecords.stream()
                .mapToInt(LotteryRecord::getPrizeValue)
                .sum();
        
        // 净收益
        int netProfit = totalWin - (int)totalCost;
        
        stats.put("totalDraws", totalDraws);
        stats.put("totalCost", totalCost);
        stats.put("totalWin", totalWin);
        stats.put("netProfit", netProfit);
        
        return stats;
    }
    
    /**
     * 获取全局抽奖统计信息（用于后台管理）
     * @return 全局统计信息
     */
    public Map<String, Object> getGlobalLotteryStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总抽奖次数
        long totalDraws = lotteryRecordMapper.selectCount(null);
        
        // 总消耗爱心币
        long totalCost = totalDraws * LOTTERY_COST;
        
        // 总获得爱心币（只统计中奖的）
        LambdaQueryWrapper<LotteryRecord> winWrapper = new LambdaQueryWrapper<>();
        winWrapper.eq(LotteryRecord::getPrizeType, "coins");
        
        List<LotteryRecord> winRecords = lotteryRecordMapper.selectList(winWrapper);
        int totalWin = winRecords.stream()
                .mapToInt(LotteryRecord::getPrizeValue)
                .sum();
        
        // 净收益
        int netProfit = totalWin - (int)totalCost;
        
        // 参与抽奖的用户数
        LambdaQueryWrapper<LotteryRecord> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.select(LotteryRecord::getUserId)
                   .groupBy(LotteryRecord::getUserId);
        long uniqueUsers = lotteryRecordMapper.selectCount(userWrapper);
        
        // 今日抽奖次数
        LambdaQueryWrapper<LotteryRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(LotteryRecord::getCreatedAt, java.time.LocalDate.now().atStartOfDay());
        long todayDraws = lotteryRecordMapper.selectCount(todayWrapper);
        
        // 中奖率（抽中爱心币的比例）
        double winRate = totalDraws > 0 ? (double)winRecords.size() / totalDraws * 100 : 0;
        
        stats.put("totalDraws", totalDraws);
        stats.put("totalCost", totalCost);
        stats.put("totalWin", totalWin);
        stats.put("netProfit", netProfit);
        stats.put("uniqueUsers", uniqueUsers);
        stats.put("todayDraws", todayDraws);
        stats.put("winRate", String.format("%.1f", winRate));
        stats.put("winCount", winRecords.size());
        
        return stats;
    }
}
