package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.BettingRecord;
import com.wrt.excel.order.entity.UserDailyBettingStats;
import com.wrt.excel.order.mapper.BettingRecordMapper;
import com.wrt.excel.order.mapper.UserDailyBettingStatsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 幸运币押注服务
 */
@Slf4j
@Service
public class BettingService {
    
    @Autowired
    private BettingRecordMapper bettingRecordMapper;
    
    @Autowired
    private UserDailyBettingStatsMapper dailyStatsMapper;
    
    @Autowired
    private CoinService coinService;
    
    // 每日最大押注次数
    private static final int MAX_DAILY_BETS = 3;
    
    // 单日累计损失锁定阈值
    private static final int DAILY_LOCK_THRESHOLD = 100;
    
    // 获胜倍率
    private static final double WIN_MULTIPLIER = 1.8;
    
    /**
     * 执行押注
     * @param userId 用户ID
     * @param betAmount 下注金额
     * @param betType 押注类型：big/small
     * @return 押注结果
     */
    @Transactional
    public Map<String, Object> placeBet(Long userId, Integer betAmount, String betType) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 验证参数
        if (!"big".equals(betType) && !"small".equals(betType)) {
            result.put("success", false);
            result.put("message", "押注类型必须是 big 或 small");
            return result;
        }
        
        if (betAmount <= 0) {
            result.put("success", false);
            result.put("message", "下注金额必须大于0");
            return result;
        }
        
        // 2. 检查余额
        Integer balance = coinService.getBalance(userId);
        if (balance < betAmount) {
            result.put("success", false);
            result.put("message", "爱心币余额不足");
            result.put("balance", balance);
            return result;
        }
        
        // 3. 检查下注金额是否超过余额的30%
        if (betAmount > balance * 0.3) {
            result.put("success", false);
            result.put("message", "下注金额不能超过当前余额的30%");
            result.put("maxBet", (int)(balance * 0.3));
            return result;
        }
        
        // 4. 检查每日押注次数和锁定状态
        LocalDate today = LocalDate.now();
        UserDailyBettingStats dailyStats = getOrCreateDailyStats(userId, today);
        
        if (dailyStats.getIsLocked() == 1) {
            result.put("success", false);
            result.put("message", "今日累计损失已超过100币，已锁定押注功能，明天再战吧~ 💕");
            result.put("isLocked", true);
            return result;
        }
        
        if (dailyStats.getTotalBets() >= MAX_DAILY_BETS) {
            result.put("success", false);
            result.put("message", "今日已达最大押注次数（3次），明天再来吧~ 💕");
            return result;
        }
        
        // 5. 扣除下注金额
        try {
            coinService.useCoins(userId, betAmount, "use_betting", 
                "幸运币押注 - 押" + ("big".equals(betType) ? "大" : "小"), null);
        } catch (Exception e) {
            log.error("扣除爱心币失败", e);
            result.put("success", false);
            result.put("message", "押注失败，请重试");
            return result;
        }
        
        // 6. 掷骰子（1-6）
        Random random = new Random();
        int diceResult = random.nextInt(6) + 1;
        
        // 7. 判断结果类型
        String resultType = diceResult >= 4 ? "big" : "small";
        
        // 8. 判断是否获胜
        boolean isWin = betType.equals(resultType);
        
        // 9. 计算奖励或扣除
        int rewardAmount = 0;
        int deductAmount = betAmount;
        boolean isHalfDeduct = false;
        
        if (isWin) {
            // 猜对：获得下注 * 1.8 爱心币（向上取整）
            rewardAmount = (int) Math.ceil(betAmount * WIN_MULTIPLIER);
            
            // 增加奖励
            coinService.addCoins(userId, rewardAmount, 
                "幸运币押注获胜 - 骰子点数：" + diceResult + "（" + getResultTypeName(resultType) + "）", "betting_win");
        } else {
            // 猜错：检查安全网
            int consecutiveLoss = dailyStats.getConsecutiveLoss() + 1;
            
            // 连续输2次，第3次保底"输了只扣一半"
            if (consecutiveLoss == 3) {
                isHalfDeduct = true;
                deductAmount = (int) Math.ceil(betAmount / 2.0);
                
                // 退还一半的金额
                coinService.addCoins(userId, betAmount - deductAmount, 
                    "幸运币押注安全网 - 连续输3次，退还一半", "betting_refund");
                
                log.info("用户 {} 触发安全网，连续输3次，只扣除一半：{}", userId, deductAmount);
            }
        }
        
        // 10. 更新统计数据
        updateDailyStats(dailyStats, betAmount, isWin, rewardAmount, deductAmount, isHalfDeduct);
        
        // 11. 记录押注
        BettingRecord record = new BettingRecord();
        record.setUserId(userId);
        record.setBetAmount(betAmount);
        record.setBetType(betType);
        record.setDiceResult(diceResult);
        record.setResultType(resultType);
        record.setIsWin(isWin ? 1 : 0);
        record.setRewardAmount(rewardAmount);
        record.setDeductAmount(deductAmount);
        record.setIsHalfDeduct(isHalfDeduct ? 1 : 0);
        record.setConsecutiveLoss(dailyStats.getConsecutiveLoss());
        record.setDailyTotalLoss(dailyStats.getTotalLoss());
        
        bettingRecordMapper.insert(record);
        
        // 12. 构建返回结果
        String message;
        if (isWin) {
            message = "恭喜！猜对了！🎉 骰子点数：" + diceResult + "（" + getResultTypeName(resultType) + 
                     "），获得 " + rewardAmount + " 爱心币！";
        } else {
            if (isHalfDeduct) {
                message = "很遗憾，猜错了。😊 骰子点数：" + diceResult + "（" + getResultTypeName(resultType) + 
                         "），但触发安全网，只扣除 " + deductAmount + " 爱心币（原应扣除 " + betAmount + "）";
            } else {
                message = "很遗憾，猜错了。😢 骰子点数：" + diceResult + "（" + getResultTypeName(resultType) + 
                         "），扣除 " + deductAmount + " 爱心币";
            }
        }
        
        log.info("用户 {} 押注：下注 {}，押{}，结果{}，点数{}，{}，新余额：{}", 
                userId, betAmount, betType, isWin ? "胜" : "负", diceResult, 
                isHalfDeduct ? "半扣" : "正常", coinService.getBalance(userId));
        
        result.put("success", true);
        result.put("message", message);
        result.put("diceResult", diceResult);
        result.put("resultType", resultType);
        result.put("isWin", isWin);
        result.put("rewardAmount", rewardAmount);
        result.put("deductAmount", deductAmount);
        result.put("isHalfDeduct", isHalfDeduct);
        result.put("newBalance", coinService.getBalance(userId));
        result.put("consecutiveLoss", dailyStats.getConsecutiveLoss());
        result.put("dailyTotalLoss", dailyStats.getTotalLoss());
        result.put("dailyBetsRemaining", MAX_DAILY_BETS - dailyStats.getTotalBets());
        result.put("isLocked", dailyStats.getIsLocked() == 1);
        
        return result;
    }
    
    /**
     * 获取或创建每日统计
     */
    private UserDailyBettingStats getOrCreateDailyStats(Long userId, LocalDate date) {
        LambdaQueryWrapper<UserDailyBettingStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDailyBettingStats::getUserId, userId)
               .eq(UserDailyBettingStats::getBetDate, date);
        
        UserDailyBettingStats stats = dailyStatsMapper.selectOne(wrapper);
        
        if (stats == null) {
            stats = new UserDailyBettingStats();
            stats.setUserId(userId);
            stats.setBetDate(date);
            stats.setTotalBets(0);
            stats.setWinCount(0);
            stats.setLossCount(0);
            stats.setTotalBetAmount(0);
            stats.setTotalReward(0);
            stats.setTotalLoss(0);
            stats.setConsecutiveLoss(0);
            stats.setIsLocked(0);
            dailyStatsMapper.insert(stats);
        }
        
        return stats;
    }
    
    /**
     * 更新每日统计
     */
    private void updateDailyStats(UserDailyBettingStats stats, int betAmount, 
                                  boolean isWin, int rewardAmount, int deductAmount, 
                                  boolean isHalfDeduct) {
        stats.setTotalBets(stats.getTotalBets() + 1);
        
        if (isWin) {
            stats.setWinCount(stats.getWinCount() + 1);
            stats.setConsecutiveLoss(0); // 重置连续输计数
            stats.setTotalReward(stats.getTotalReward() + rewardAmount);
        } else {
            stats.setLossCount(stats.getLossCount() + 1);
            stats.setConsecutiveLoss(stats.getConsecutiveLoss() + 1);
            stats.setTotalLoss(stats.getTotalLoss() + deductAmount);
        }
        
        stats.setTotalBetAmount(stats.getTotalBetAmount() + betAmount);
        
        // 检查是否需要锁定
        if (stats.getTotalLoss() >= DAILY_LOCK_THRESHOLD) {
            stats.setIsLocked(1);
        }
        
        dailyStatsMapper.updateById(stats);
    }
    
    /**
     * 获取结果类型名称
     */
    private String getResultTypeName(String resultType) {
        return "big".equals(resultType) ? "大" : "小";
    }
    
    /**
     * 获取用户的押注历史
     * @param userId 用户ID
     * @return 押注记录列表
     */
    public java.util.List<BettingRecord> getUserBettingHistory(Long userId) {
        LambdaQueryWrapper<BettingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BettingRecord::getUserId, userId)
               .orderByDesc(BettingRecord::getCreatedAt)
               .last("LIMIT 50"); // 最近50条
        
        return bettingRecordMapper.selectList(wrapper);
    }
    
    /**
     * 获取用户的押注统计
     * @param userId 用户ID
     * @return 统计信息
     */
    public Map<String, Object> getUserBettingStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 今日统计
        LocalDate today = LocalDate.now();
        UserDailyBettingStats dailyStats = getOrCreateDailyStats(userId, today);
        
        stats.put("todayBets", dailyStats.getTotalBets());
        stats.put("todayWins", dailyStats.getWinCount());
        stats.put("todayLosses", dailyStats.getLossCount());
        stats.put("todayTotalBet", dailyStats.getTotalBetAmount());
        stats.put("todayTotalReward", dailyStats.getTotalReward());
        stats.put("todayTotalLoss", dailyStats.getTotalLoss());
        stats.put("consecutiveLoss", dailyStats.getConsecutiveLoss());
        stats.put("isLocked", dailyStats.getIsLocked() == 1);
        stats.put("betsRemaining", MAX_DAILY_BETS - dailyStats.getTotalBets());
        
        // 总统计
        LambdaQueryWrapper<BettingRecord> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(BettingRecord::getUserId, userId);
        long totalBets = bettingRecordMapper.selectCount(totalWrapper);
        
        LambdaQueryWrapper<BettingRecord> winWrapper = new LambdaQueryWrapper<>();
        winWrapper.eq(BettingRecord::getUserId, userId)
                  .eq(BettingRecord::getIsWin, 1);
        long totalWins = bettingRecordMapper.selectCount(winWrapper);
        
        stats.put("totalBets", totalBets);
        stats.put("totalWins", totalWins);
        stats.put("totalLosses", totalBets - totalWins);
        
        // 胜率
        double winRate = totalBets > 0 ? (double)totalWins / totalBets * 100 : 0;
        stats.put("winRate", String.format("%.1f", winRate));
        
        return stats;
    }
}
