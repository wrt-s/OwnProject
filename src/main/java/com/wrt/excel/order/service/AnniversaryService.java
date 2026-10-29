package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.Anniversary;
import com.wrt.excel.order.mapper.AnniversaryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 纪念日服务
 */
@Slf4j
@Service
public class AnniversaryService {
    
    @Autowired
    private AnniversaryMapper anniversaryMapper;
    
    /**
     * 添加纪念日
     */
    public Anniversary getById(Long id) {
        return anniversaryMapper.selectById(id);
    }
    
    public void addAnniversary(Anniversary anniversary) {
        if (anniversary.getIsActive() == null) {
            anniversary.setIsActive(1);
        }
        anniversaryMapper.insert(anniversary);
        log.info("添加纪念日: {}", anniversary.getName());
    }
    
    /**
     * 更新纪念日
     */
    public void updateAnniversary(Anniversary anniversary) {
        anniversaryMapper.updateById(anniversary);
        log.info("更新纪念日: {}", anniversary.getId());
    }
    
    /**
     * 删除纪念日
     */
    public void deleteAnniversary(Long id) {
        anniversaryMapper.deleteById(id);
        log.info("删除纪念日: {}", id);
    }
    
    /**
     * 获取用户所有纪念日
     */
    public List<Anniversary> getUserAnniversaries(Long familyId) {
        LambdaQueryWrapper<Anniversary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Anniversary::getFamilyId, familyId)
               .eq(Anniversary::getIsActive, 1)
               .orderByAsc(Anniversary::getDate);
        
        return anniversaryMapper.selectList(wrapper);
    }
    
    /**
     * 获取即将到来的纪念日（最近N天）
     */
    public List<Anniversary> getUpcomingAnniversaries(Long familyId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);
        
        List<Anniversary> allAnniversaries = getUserAnniversaries(familyId);
        
        return allAnniversaries.stream()
                .filter(a -> {
                    // 计算今年的纪念日日期
                    LocalDate thisYearDate = a.getDate().withYear(today.getYear());
                    
                    // 如果今年的日期已过，则看明年
                    if (thisYearDate.isBefore(today)) {
                        thisYearDate = thisYearDate.plusYears(1);
                    }
                    
                    // 判断是否在范围内
                    return !thisYearDate.isAfter(endDate);
                })
                .sorted((a1, a2) -> {
                    LocalDate date1 = a1.getDate().withYear(today.getYear());
                    if (date1.isBefore(today)) {
                        date1 = date1.plusYears(1);
                    }
                    LocalDate date2 = a2.getDate().withYear(today.getYear());
                    if (date2.isBefore(today)) {
                        date2 = date2.plusYears(1);
                    }
                    return date1.compareTo(date2);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 计算距离纪念日的天数
     */
    public int getDaysUntilAnniversary(Anniversary anniversary) {
        LocalDate today = LocalDate.now();
        LocalDate nextDate = anniversary.getDate().withYear(today.getYear());
        // Upcoming only: today = 0; if already past this year, roll to next year
        if (nextDate.isBefore(today)) {
            nextDate = nextDate.plusYears(1);
        }
        return (int) ChronoUnit.DAYS.between(today, nextDate);
    }
}
