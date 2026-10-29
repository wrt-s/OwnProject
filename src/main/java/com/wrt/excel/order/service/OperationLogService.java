package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.dto.OperationLogStatsDTO;
import com.wrt.excel.order.dto.UserOperationStatsDTO;
import com.wrt.excel.order.entity.OperationLog;
import com.wrt.excel.order.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作日志服务类
 */
@Service
@Slf4j
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;
    
    /**
     * 默认日志保留天数（30天）
     */
    private static final int DEFAULT_RETENTION_DAYS = 30;

    /**
     * 保存操作日志
     */
    public void saveLog(OperationLog operationLog) {
        try {
            operationLog.setOperationTime(LocalDateTime.now());
            operationLog.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(operationLog);
            log.debug("操作日志保存成功: {}", operationLog.getDescription());
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    /**
     * 查询操作日志列表
     */
    public List<OperationLog> getLogList(Integer pageNum, Integer pageSize, String module, 
                                         String operationType, Long userId,
                                         String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (module != null && !module.isEmpty()) {
            wrapper.like(OperationLog::getModule, module);
        }
        
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        
        wrapper.orderByDesc(OperationLog::getOperationTime);
        
        // 分页查询
        int offset = (pageNum - 1) * pageSize;
        wrapper.last("LIMIT " + offset + ", " + pageSize);
        
        return operationLogMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询操作日志
     */
    public OperationLog getLogById(Long id) {
        return operationLogMapper.selectById(id);
    }

    /**
     * 删除操作日志
     */
    public void deleteLog(Long id) {
        operationLogMapper.deleteById(id);
    }

    /**
     * 清空操作日志
     */
    public void clearLogs() {
        operationLogMapper.delete(null);
    }

    /**
     * 统计操作日志数量
     */
    public Long countLogs(String module, String operationType, Long userId,
                         String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (module != null && !module.isEmpty()) {
            wrapper.like(OperationLog::getModule, module);
        }
        
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        
        return operationLogMapper.selectCount(wrapper);
    }
    
    /**
     * 获取操作类型统计
     */
    public Map<String, Object> getOperationTypeStats(String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        
        // 按操作类型分组统计
        Map<String, Long> typeCount = logs.stream()
            .collect(Collectors.groupingBy(
                log -> log.getOperationType() != null ? log.getOperationType() : "未知",
                Collectors.counting()
            ));
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", new ArrayList<>(typeCount.keySet()));
        result.put("data", new ArrayList<>(typeCount.values()));
        
        return result;
    }
    
    /**
     * 获取近7天操作趋势
     */
    public Map<String, Object> getOperationTrendStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(OperationLog::getOperationTime, sevenDaysAgo);
        wrapper.le(OperationLog::getOperationTime, now);
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        
        // 按日期分组统计
        Map<String, Long> dailyCount = logs.stream()
            .collect(Collectors.groupingBy(
                log -> log.getOperationTime().format(DateTimeFormatter.ofPattern("MM-dd")),
                Collectors.counting()
            ));
        
        // 确保包含最近7天的所有日期
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = now.minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd"));
            labels.add(date);
            data.add(dailyCount.getOrDefault(date, 0L));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        
        return result;
    }
    
    /**
     * 获取模块分布统计
     */
    public Map<String, Object> getModuleDistributionStats(String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        
        // 按模块分组统计
        Map<String, Long> moduleCount = logs.stream()
            .collect(Collectors.groupingBy(
                log -> log.getModule() != null ? log.getModule() : "未知",
                Collectors.counting()
            ));
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", new ArrayList<>(moduleCount.keySet()));
        result.put("data", new ArrayList<>(moduleCount.values()));
        
        return result;
    }
    
    /**
     * 获取成功率统计
     */
    public Map<String, Object> getSuccessRateStats(String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        
        long successCount = logs.stream().filter(log -> log.getStatus() != null && log.getStatus() == 1).count();
        long failCount = logs.stream().filter(log -> log.getStatus() != null && log.getStatus() == 0).count();
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", Arrays.asList("成功", "失败"));
        result.put("data", Arrays.asList(successCount, failCount));
        
        return result;
    }
    
    /**
     * 获取综合统计数据
     */
    public OperationLogStatsDTO getComprehensiveStats(String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        
        OperationLogStatsDTO stats = new OperationLogStatsDTO();
        stats.setTotalOperations((long) logs.size());
        
        long successCount = logs.stream().filter(log -> log.getStatus() != null && log.getStatus() == 1).count();
        long failedCount = logs.stream().filter(log -> log.getStatus() != null && log.getStatus() == 0).count();
        
        stats.setSuccessOperations(successCount);
        stats.setFailedOperations(failedCount);
        
        // 计算成功率
        if (stats.getTotalOperations() > 0) {
            stats.setSuccessRate((double) successCount / stats.getTotalOperations() * 100);
        } else {
            stats.setSuccessRate(0.0);
        }
        
        // 计算耗时统计
        List<Long> costTimes = logs.stream()
            .filter(log -> log.getCostTime() != null)
            .map(OperationLog::getCostTime)
            .collect(Collectors.toList());
        
        if (!costTimes.isEmpty()) {
            stats.setAvgCostTime(costTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
            stats.setMaxCostTime(Collections.max(costTimes));
            stats.setMinCostTime(Collections.min(costTimes));
        } else {
            stats.setAvgCostTime(0.0);
            stats.setMaxCostTime(0L);
            stats.setMinCostTime(0L);
        }
        
        return stats;
    }
    
    /**
     * 获取TOP用户操作统计
     */
    public List<UserOperationStatsDTO> getTopUserStats(int topN, String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        
        // 只查询有用户ID的记录
        wrapper.isNotNull(OperationLog::getUserId);
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        
        // 按用户分组统计
        Map<Long, List<OperationLog>> userLogsMap = logs.stream()
            .collect(Collectors.groupingBy(OperationLog::getUserId));
        
        List<UserOperationStatsDTO> userStats = new ArrayList<>();
        for (Map.Entry<Long, List<OperationLog>> entry : userLogsMap.entrySet()) {
            Long userId = entry.getKey();
            List<OperationLog> userLogs = entry.getValue();
            
            long operationCount = userLogs.size();
            long successCount = userLogs.stream()
                .filter(log -> log.getStatus() != null && log.getStatus() == 1)
                .count();
            long failedCount = userLogs.stream()
                .filter(log -> log.getStatus() != null && log.getStatus() == 0)
                .count();
            
            // 获取用户名（取第一条记录的用户名）
            String username = userLogs.stream()
                .map(OperationLog::getUsername)
                .filter(name -> name != null && !name.isEmpty())
                .findFirst()
                .orElse("未知用户");
            
            userStats.add(new UserOperationStatsDTO(userId, username, operationCount, successCount, failedCount));
        }
        
        // 按操作次数排序，取前N个
        return userStats.stream()
            .sorted((a, b) -> Long.compare(b.getOperationCount(), a.getOperationCount()))
            .limit(topN)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取高频操作统计
     */
    public Map<String, Object> getHighFrequencyOperations(String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);
        
        // 按方法名分组统计
        Map<String, Long> methodCount = logs.stream()
            .filter(log -> log.getMethod() != null)
            .collect(Collectors.groupingBy(
                OperationLog::getMethod,
                Collectors.counting()
            ));
        
        // 取前10个高频操作
        List<Map<String, Object>> topOperations = methodCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(entry -> {
                Map<String, Object> op = new HashMap<>();
                op.put("method", entry.getKey());
                op.put("count", entry.getValue());
                return op;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("topOperations", topOperations);
        result.put("totalUniqueOperations", methodCount.size());
        
        return result;
    }
    
    /**
     * 获取异常操作统计（失败的操作）
     */
    public Map<String, Object> getAbnormalOperations(String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getStatus, 0); // 只查询失败的操作
        
        // 时间范围筛选
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        
        wrapper.orderByDesc(OperationLog::getOperationTime);
        wrapper.last("LIMIT 50"); // 最多返回50条
        
        List<OperationLog> abnormalLogs = operationLogMapper.selectList(wrapper);
        
        // 按错误类型分组
        Map<String, Long> errorTypeCount = abnormalLogs.stream()
            .filter(log -> log.getErrorMsg() != null)
            .collect(Collectors.groupingBy(
                log -> {
                    String errorMsg = log.getErrorMsg();
                    // 提取异常类型（简单处理）
                    if (errorMsg.contains(":")) {
                        return errorMsg.substring(0, errorMsg.indexOf(":"));
                    }
                    return errorMsg.length() > 50 ? errorMsg.substring(0, 50) : errorMsg;
                },
                Collectors.counting()
            ));
        
        Map<String, Object> result = new HashMap<>();
        result.put("abnormalLogs", abnormalLogs);
        result.put("errorTypeDistribution", errorTypeCount);
        result.put("totalAbnormalCount", abnormalLogs.size());
        
        return result;
    }
    
    /**
     * 定时清理过期日志（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredLogs() {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(DEFAULT_RETENTION_DAYS);
            LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(OperationLog::getOperationTime, expireTime);
            
            int deletedCount = operationLogMapper.delete(wrapper);
            log.info("清理过期操作日志完成，删除 {} 条记录", deletedCount);
        } catch (Exception e) {
            log.error("清理过期操作日志失败", e);
        }
    }
    
    /**
     * 手动清理过期日志
     */
    public int cleanExpiredLogsManually(int retentionDays) {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);
            LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(OperationLog::getOperationTime, expireTime);
            
            int deletedCount = operationLogMapper.delete(wrapper);
            log.info("手动清理过期操作日志完成，删除 {} 条记录", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("手动清理过期操作日志失败", e);
            throw e;
        }
    }
}
