package com.wrt.excel.order.controller;

import com.wrt.excel.order.dto.OperationLogStatsDTO;
import com.wrt.excel.order.dto.UserOperationStatsDTO;
import com.wrt.excel.order.entity.OperationLog;
import com.wrt.excel.order.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/api/operation-log")
@Slf4j
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 查询操作日志列表
     */
    @GetMapping("/list")
    public Map<String, Object> getLogList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            List<OperationLog> logs = operationLogService.getLogList(pageNum, pageSize, module, operationType, userId, startTime, endTime);
            Long total = operationLogService.countLogs(module, operationType, userId, startTime, endTime);
            
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", logs);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
        } catch (Exception e) {
            log.error("查询操作日志失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID查询操作日志详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getLogById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            OperationLog log = operationLogService.getLogById(id);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", log);
        } catch (Exception e) {
            log.error("查询操作日志详情失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 删除操作日志
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteLog(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            operationLogService.deleteLog(id);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            log.error("删除操作日志失败", e);
            result.put("code", 500);
            result.put("message", "删除失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 清空所有操作日志
     */
    @DeleteMapping("/clear")
    public Map<String, Object> clearLogs() {
        Map<String, Object> result = new HashMap<>();
        try {
            operationLogService.clearLogs();
            result.put("code", 200);
            result.put("message", "清空成功");
        } catch (Exception e) {
            log.error("清空操作日志失败", e);
            result.put("code", 500);
            result.put("message", "清空失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 统计操作日志数量
     */
    @GetMapping("/count")
    public Map<String, Object> countLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Long count = operationLogService.countLogs(module, operationType, userId, startTime, endTime);
            result.put("code", 200);
            result.put("message", "统计成功");
            result.put("data", count);
        } catch (Exception e) {
            log.error("统计操作日志失败", e);
            result.put("code", 500);
            result.put("message", "统计失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取操作类型统计
     */
    @GetMapping("/stats/type")
    public Map<String, Object> getOperationTypeStats(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = operationLogService.getOperationTypeStats(startTime, endTime);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("查询操作类型统计失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取操作趋势统计
     */
    @GetMapping("/stats/trend")
    public Map<String, Object> getOperationTrendStats() {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = operationLogService.getOperationTrendStats();
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("查询操作趋势统计失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取模块分布统计
     */
    @GetMapping("/stats/module")
    public Map<String, Object> getModuleDistributionStats(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = operationLogService.getModuleDistributionStats(startTime, endTime);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("查询模块分布统计失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取成功率统计
     */
    @GetMapping("/stats/success-rate")
    public Map<String, Object> getSuccessRateStats(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = operationLogService.getSuccessRateStats(startTime, endTime);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("查询成功率统计失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取综合统计数据
     */
    @GetMapping("/stats/comprehensive")
    public Map<String, Object> getComprehensiveStats(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            OperationLogStatsDTO stats = operationLogService.getComprehensiveStats(startTime, endTime);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("查询综合统计数据失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取TOP用户操作统计
     */
    @GetMapping("/stats/top-users")
    public Map<String, Object> getTopUserStats(
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            List<UserOperationStatsDTO> stats = operationLogService.getTopUserStats(topN, startTime, endTime);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("查询TOP用户统计失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取高频操作统计
     */
    @GetMapping("/stats/high-frequency")
    public Map<String, Object> getHighFrequencyOperations(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = operationLogService.getHighFrequencyOperations(startTime, endTime);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("查询高频操作统计失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取异常操作统计
     */
    @GetMapping("/stats/abnormal")
    public Map<String, Object> getAbnormalOperations(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = operationLogService.getAbnormalOperations(startTime, endTime);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("查询异常操作统计失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 手动清理过期日志
     */
    @PostMapping("/clean-expired")
    public Map<String, Object> cleanExpiredLogs(
            @RequestParam(defaultValue = "30") int retentionDays) {
        
        Map<String, Object> result = new HashMap<>();
        try {
            int deletedCount = operationLogService.cleanExpiredLogsManually(retentionDays);
            result.put("code", 200);
            result.put("message", "清理成功");
            result.put("data", deletedCount);
        } catch (Exception e) {
            log.error("清理过期日志失败", e);
            result.put("code", 500);
            result.put("message", "清理失败: " + e.getMessage());
        }
        return result;
    }
}
