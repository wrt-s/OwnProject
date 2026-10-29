package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.CoinBalance;
import com.wrt.excel.order.entity.CoinTransaction;
import com.wrt.excel.order.mapper.CoinBalanceMapper;
import com.wrt.excel.order.mapper.CoinTransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 爱心币服务
 */
@Slf4j
@Service
public class CoinService {
    
    @Autowired
    private CoinBalanceMapper coinBalanceMapper;
    
    @Autowired
    private CoinTransactionMapper coinTransactionMapper;
    
    /**
     * 获取用户爱心币余额
     */
    public Integer getBalance(Long userId) {
        LambdaQueryWrapper<CoinBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoinBalance::getUserId, userId);
        CoinBalance balance = coinBalanceMapper.selectOne(wrapper);
        
        if (balance == null) {
            // 如果不存在，创建初始余额为0的记录
            balance = new CoinBalance();
            balance.setUserId(userId);
            balance.setBalance(0);
            coinBalanceMapper.insert(balance);
            return 0;
        }
        
        return balance.getBalance();
    }
    
    /**
     * 增加爱心币（通过"坏坏"方式）- 保持向后兼容
     */
    @Transactional
    public void addCoins(Long userId, Integer amount, String description) {
        addCoins(userId, amount, description, "坏坏");
    }
    
    /**
     * 增加爱心币（通过不同方式）
     */
    @Transactional
    public void addCoins(Long userId, Integer amount, String description, String type) {
        if (amount <= 0) {
            throw new IllegalArgumentException("增加的爱心币数量必须大于0");
        }
        
        // 更新余额
        LambdaQueryWrapper<CoinBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoinBalance::getUserId, userId);
        CoinBalance balance = coinBalanceMapper.selectOne(wrapper);
        
        if (balance == null) {
            balance = new CoinBalance();
            balance.setUserId(userId);
            balance.setBalance(amount);
            coinBalanceMapper.insert(balance);
        } else {
            balance.setBalance(balance.getBalance() + amount);
            coinBalanceMapper.updateById(balance);
        }
        
        // 记录流水
        CoinTransaction transaction = new CoinTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balance.getBalance());
        transaction.setType("add_" + type); // 使用传入的类型参数
        transaction.setDescription(description != null ? description : "通过" + type + "方式增加");
        transaction.setCreatedAt(LocalDateTime.now());
        coinTransactionMapper.insert(transaction);
        
        log.info("用户 {} 通过 {} 方式增加 {} 爱心币，当前余额: {}", userId, type, amount, balance.getBalance());
    }
    
    /**
     * 使用爱心币（点单或任务）
     */
    @Transactional
    public void useCoins(Long userId, Integer amount, String type, String description, Long relatedId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("使用的爱心币数量必须大于0");
        }
        
        // 行锁查询当前余额，避免并发扣成负数
        LambdaQueryWrapper<CoinBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoinBalance::getUserId, userId)
               .last("FOR UPDATE");
        CoinBalance balance = coinBalanceMapper.selectOne(wrapper);
        
        if (balance == null || balance.getBalance() == null || balance.getBalance() < amount) {
            throw new IllegalArgumentException("爱心币余额不足");
        }
        
        // 更新余额
        balance.setBalance(balance.getBalance() - amount);
        coinBalanceMapper.updateById(balance);
        
        // 记录流水
        CoinTransaction transaction = new CoinTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(-amount);
        transaction.setBalanceAfter(balance.getBalance());
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setRelatedId(relatedId);
        transaction.setCreatedAt(LocalDateTime.now());
        coinTransactionMapper.insert(transaction);
        
        log.info("用户 {} 使用 {} 爱心币，当前余额: {}", userId, amount, balance.getBalance());
    }
    
    /**
     * 退还爱心币（订单取消等场景）
     */
    @Transactional
    public void refundCoins(Long userId, Integer amount, String type, String description, Long relatedId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("退还的爱心币数量必须大于0");
        }
        
        // 更新余额
        LambdaQueryWrapper<CoinBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoinBalance::getUserId, userId);
        CoinBalance balance = coinBalanceMapper.selectOne(wrapper);
        
        if (balance == null) {
            balance = new CoinBalance();
            balance.setUserId(userId);
            balance.setBalance(amount);
            coinBalanceMapper.insert(balance);
        } else {
            balance.setBalance(balance.getBalance() + amount);
            coinBalanceMapper.updateById(balance);
        }
        
        // 记录流水
        CoinTransaction transaction = new CoinTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balance.getBalance());
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setRelatedId(relatedId);
        transaction.setCreatedAt(LocalDateTime.now());
        coinTransactionMapper.insert(transaction);
        
        log.info("用户 {} 退还 {} 爱心币，当前余额: {}", userId, amount, balance.getBalance());
    }
    
    /**
     * 获取用户爱心币流水记录
     */
    public List<CoinTransaction> getTransactions(Long userId) {
        LambdaQueryWrapper<CoinTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoinTransaction::getUserId, userId)
               .orderByDesc(CoinTransaction::getCreatedAt);
        return coinTransactionMapper.selectList(wrapper);
    }
}
