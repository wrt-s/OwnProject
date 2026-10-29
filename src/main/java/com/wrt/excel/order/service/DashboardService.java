package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.entity.Order;
import com.wrt.excel.order.entity.OrderItem;
import com.wrt.excel.order.mapper.DishMapper;
import com.wrt.excel.order.mapper.OrderItemMapper;
import com.wrt.excel.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据看板服务
 */
@Slf4j
@Service
public class DashboardService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private DishMapper dishMapper;

    /**
     * 获取订单量趋势（每日/每周/每月）
     */
    public Map<String, Object> getOrderTrend(String type) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> trendData = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "completed");

        if ("daily".equals(type)) {
            // 最近7天
            LocalDateTime startTime = now.minusDays(6).withHour(0).withMinute(0).withSecond(0);
            wrapper.ge(Order::getCreatedAt, startTime);
            List<Order> orders = orderMapper.selectList(wrapper);

            Map<String, Long> dateCountMap = orders.stream()
                    .collect(Collectors.groupingBy(
                            o -> o.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd")),
                            Collectors.counting()
                    ));

            for (int i = 6; i >= 0; i--) {
                String date = now.minusDays(i).toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd"));
                Map<String, Object> item = new HashMap<>();
                item.put("date", date);
                item.put("count", dateCountMap.getOrDefault(date, 0L));
                trendData.add(item);
            }
        } else if ("weekly".equals(type)) {
            // 最近8周
            for (int i = 7; i >= 0; i--) {
                LocalDateTime weekStart = now.minusWeeks(i).with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime weekEnd = weekStart.plusWeeks(1);
                
                LambdaQueryWrapper<Order> weekWrapper = new LambdaQueryWrapper<>();
                weekWrapper.eq(Order::getStatus, "completed")
                        .ge(Order::getCreatedAt, weekStart)
                        .lt(Order::getCreatedAt, weekEnd);
                Long count = orderMapper.selectCount(weekWrapper);

                Map<String, Object> item = new HashMap<>();
                item.put("date", "第" + (now.minusWeeks(i).get(java.time.temporal.WeekFields.of(Locale.CHINA).weekOfYear())) + "周");
                item.put("count", count);
                trendData.add(item);
            }
        } else if ("monthly".equals(type)) {
            // 最近6个月
            for (int i = 5; i >= 0; i--) {
                LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime monthEnd = monthStart.plusMonths(1);
                
                LambdaQueryWrapper<Order> monthWrapper = new LambdaQueryWrapper<>();
                monthWrapper.eq(Order::getStatus, "completed")
                        .ge(Order::getCreatedAt, monthStart)
                        .lt(Order::getCreatedAt, monthEnd);
                Long count = orderMapper.selectCount(monthWrapper);

                Map<String, Object> item = new HashMap<>();
                item.put("date", monthStart.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
                item.put("count", count);
                trendData.add(item);
            }
        }

        result.put("trend", trendData);
        return result;
    }

    /**
     * 获取热门菜品排行榜
     */
    public Map<String, Object> getPopularDishes(int limit) {
        Map<String, Object> result = new HashMap<>();
        
        // 查询所有已完成订单的订单项
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, "completed");
        List<Order> completedOrders = orderMapper.selectList(orderWrapper);
        
        if (completedOrders.isEmpty()) {
            result.put("dishes", new ArrayList<>());
            return result;
        }

        List<Long> orderIds = completedOrders.stream().map(Order::getId).collect(Collectors.toList());
        
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(OrderItem::getOrderId, orderIds);
        List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);

        // 统计每个菜品的数量
        Map<Long, Long> dishCountMap = orderItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getDishId,
                        Collectors.summingLong(OrderItem::getQuantity)
                ));

        // 获取菜品信息并排序
        List<Map<String, Object>> popularDishes = dishCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Dish dish = dishMapper.selectById(entry.getKey());
                    Map<String, Object> item = new HashMap<>();
                    if (dish != null) {
                        item.put("id", dish.getId());
                        item.put("name", dish.getName());
                        item.put("imageUrl", dish.getImageUrl());
                        item.put("count", entry.getValue());
                    }
                    return item;
                })
                .filter(item -> item.containsKey("id"))
                .collect(Collectors.toList());

        result.put("dishes", popularDishes);
        return result;
    }

    /**
     * 获取最爱时间段分析
     */
    public Map<String, Object> getTimeSlotAnalysis() {
        Map<String, Object> result = new HashMap<>();
        
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "completed");
        List<Order> orders = orderMapper.selectList(wrapper);

        Map<String, Long> timeSlotCount = new HashMap<>();
        timeSlotCount.put("morning", 0L);    // 6:00-12:00
        timeSlotCount.put("afternoon", 0L);  // 12:00-18:00
        timeSlotCount.put("evening", 0L);    // 18:00-22:00
        timeSlotCount.put("night", 0L);      // 22:00-6:00

        for (Order order : orders) {
            int hour = order.getCreatedAt().getHour();
            if (hour >= 6 && hour < 12) {
                timeSlotCount.put("morning", timeSlotCount.get("morning") + 1);
            } else if (hour >= 12 && hour < 18) {
                timeSlotCount.put("afternoon", timeSlotCount.get("afternoon") + 1);
            } else if (hour >= 18 && hour < 22) {
                timeSlotCount.put("evening", timeSlotCount.get("evening") + 1);
            } else {
                timeSlotCount.put("night", timeSlotCount.get("night") + 1);
            }
        }

        result.put("timeSlots", timeSlotCount);
        
        // 找出最爱时间段
        String favoriteSlot = timeSlotCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("morning");
        
        Map<String, String> slotNames = new HashMap<>();
        slotNames.put("morning", "早晨 (6:00-12:00)");
        slotNames.put("afternoon", "下午 (12:00-18:00)");
        slotNames.put("evening", "晚上 (18:00-22:00)");
        slotNames.put("night", "深夜 (22:00-6:00)");
        
        result.put("favoriteSlot", slotNames.get(favoriteSlot));
        
        return result;
    }

    /**
     * 获取消费金额统计
     */
    public Map<String, Object> getConsumptionStats() {
        Map<String, Object> result = new HashMap<>();
        
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "completed");
        List<Order> orders = orderMapper.selectList(wrapper);

        if (orders.isEmpty()) {
            result.put("totalAmount", 0);
            result.put("averageAmount", 0);
            result.put("maxAmount", 0);
            result.put("orderCount", 0);
            result.put("dailyStats", new ArrayList<>());
            return result;
        }

        // 总金额
        int totalAmount = orders.stream().mapToInt(Order::getTotalPrice).sum();
        
        // 平均金额
        double averageAmount = totalAmount / (double) orders.size();
        
        // 最大单笔金额
        int maxAmount = orders.stream().mapToInt(Order::getTotalPrice).max().orElse(0);

        // 最近7天每日消费统计
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = now.minusDays(i).toLocalDate();
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            LambdaQueryWrapper<Order> dayWrapper = new LambdaQueryWrapper<>();
            dayWrapper.eq(Order::getStatus, "completed")
                    .ge(Order::getCreatedAt, dayStart)
                    .lt(Order::getCreatedAt, dayEnd);
            List<Order> dayOrders = orderMapper.selectList(dayWrapper);
            
            int dayAmount = dayOrders.stream().mapToInt(Order::getTotalPrice).sum();
            
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            dayStat.put("amount", dayAmount);
            dayStat.put("count", dayOrders.size());
            dailyStats.add(dayStat);
        }

        result.put("totalAmount", totalAmount);
        result.put("averageAmount", Math.round(averageAmount * 100.0) / 100.0);
        result.put("maxAmount", maxAmount);
        result.put("orderCount", orders.size());
        result.put("dailyStats", dailyStats);

        return result;
    }
}
