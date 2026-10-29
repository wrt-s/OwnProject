package com.wrt.excel.order.controller;

import com.wrt.excel.order.dto.OrderRequestDTO;
import com.wrt.excel.order.entity.Order;
import com.wrt.excel.order.entity.OrderItem;
import com.wrt.excel.order.service.AuthService;
import com.wrt.excel.order.service.CoinService;
import com.wrt.excel.order.service.DashboardService;
import com.wrt.excel.order.service.OrderService;
import com.wrt.excel.system.exception.ApiErrors;
import com.wrt.excel.system.interfance.OperationLogAnnotation;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器（下单/取消以 Session 用户为准）
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private CoinService coinService;
    
    @Autowired
    private DashboardService dashboardService;
    
    /**
     * 创建订单 — 强制使用 Session 用户，忽略客户端 userId
     */
    @PostMapping("/create")
    @OperationLogAnnotation(
        module = "订单管理",
        operationType = "新增",
        description = "创建新订单",
        recordParams = true,
        recordResult = false
    )
    public Map<String, Object> createOrder(@RequestBody OrderRequestDTO request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (request == null) {
                result.put("code", 400);
                result.put("message", "缺少订单参数");
                return result;
            }
            Long userId = AuthService.forceSessionUserId(session, request.getUserId());
            request.setUserId(userId);
            Order order = orderService.createOrder(request);
            
            Integer balance = coinService.getBalance(userId);
            
            result.put("code", 200);
            String summary = order.getPriceSummary();
            result.put("message", summary == null || summary.isBlank()
                    ? "下单成功！马上为你准备~ 💕"
                    : ("下单成功！" + summary + " 💕"));
            result.put("data", order);
            result.put("balance", balance);
        } catch (Exception e) {
            if (!(e instanceof IllegalArgumentException) && !(e instanceof IllegalStateException)) {
                log.error("创建订单失败", e);
            }
            ApiErrors.fill(result, e);
        }
        return result;
    }
    
    /**
     * 获取用户订单列表 — ORDERER 只能看自己；CHEF 可看指定用户
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserOrders(@PathVariable Long userId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long uid = AuthService.resolveTargetUserId(session, userId);
            List<Order> orders = orderService.getUserOrders(uid);
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", orders);
        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            ApiErrors.fill(result, e);
        }
        return result;
    }
    
    /**
     * 获取所有订单列表（后台管理用）
     */
    @GetMapping("/list")
    public Map<String, Object> getAllOrders() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Order> orders = orderService.getAllOrders();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", orders);
        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            ApiErrors.fill(result, e);
        }
        return result;
    }
    
    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{orderId}")
    public Map<String, Object> getOrderDetail(@PathVariable Long orderId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                result.put("code", 404);
                result.put("message", "订单不存在");
                return result;
            }
            // ORDERER 只能看自己的；CHEF 可看全部
            Long sid = AuthService.requireSessionUserId(session);
            if (!AuthService.isChefSession(session) && !sid.equals(order.getUserId())) {
                result.put("code", 403);
                result.put("message", "无权查看此订单");
                return result;
            }
            
            List<OrderItem> orderItems = orderService.getOrderItems(orderId);
            
            Map<String, Object> orderDetail = new HashMap<>();
            orderDetail.put("order", order);
            orderDetail.put("items", orderItems);
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", orderDetail);
        } catch (Exception e) {
            log.error("获取订单详情失败", e);
            ApiErrors.fill(result, e);
        }
        return result;
    }
    
    /**
     * 更新订单状态
     */
    @PutMapping("/status")
    @OperationLogAnnotation(
        module = "订单管理",
        operationType = "修改",
        description = "更新订单状态",
        recordParams = true,
        recordResult = false
    )
    public Map<String, Object> updateOrderStatus(@RequestParam String orderNo, @RequestParam String status) {
        Map<String, Object> result = new HashMap<>();
        try {
            orderService.updateOrderStatus(orderNo, status);
            result.put("code", 200);
            result.put("message", "状态更新成功");
        } catch (Exception e) {
            if (!(e instanceof IllegalArgumentException)) {
                log.error("更新订单状态失败", e);
            }
            ApiErrors.fill(result, e);
        }
        return result;
    }
    
    /**
     * 取消订单（仅 pending；以 Session 用户为准）
     */
    @PostMapping("/cancel")
    @OperationLogAnnotation(
        module = "订单管理",
        operationType = "修改",
        description = "取消订单",
        recordParams = true,
        recordResult = false
    )
    public Map<String, Object> cancelOrder(@RequestBody Map<String, Object> params, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (params == null || params.get("orderNo") == null || String.valueOf(params.get("orderNo")).isBlank()) {
                result.put("code", 400);
                result.put("message", "缺少订单号");
                return result;
            }
            String orderNo = String.valueOf(params.get("orderNo"));
            Long clientUid = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
            Long userId = AuthService.forceSessionUserId(session, clientUid);
            
            orderService.cancelOrder(orderNo, userId);

            Integer balance = coinService.getBalance(userId);

            result.put("code", 200);
            result.put("message", "订单已取消，爱心币已退回");
            result.put("balance", balance);
        } catch (Exception e) {
            if (!(e instanceof IllegalArgumentException) && !(e instanceof IllegalStateException)) {
                log.error("取消订单失败", e);
            }
            ApiErrors.fill(result, e);
        }
        return result;
    }
    
    @GetMapping("/dashboard/trend")
    public Map<String, Object> getOrderTrend(@RequestParam(defaultValue = "daily") String type) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dashboardService.getOrderTrend(type));
        } catch (Exception e) {
            log.error("获取订单趋势失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/dashboard/popular-dishes")
    public Map<String, Object> getPopularDishes(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dashboardService.getPopularDishes(limit));
        } catch (Exception e) {
            log.error("获取热门菜品失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/dashboard/time-slots")
    public Map<String, Object> getTimeSlotAnalysis() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dashboardService.getTimeSlotAnalysis());
        } catch (Exception e) {
            log.error("获取时间段分析失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/dashboard/consumption")
    public Map<String, Object> getConsumptionStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", dashboardService.getConsumptionStats());
        } catch (Exception e) {
            log.error("获取消费统计失败", e);
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
