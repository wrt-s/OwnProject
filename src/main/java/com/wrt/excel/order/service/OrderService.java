package com.wrt.excel.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wrt.excel.order.dto.CartItemDTO;
import com.wrt.excel.order.dto.OrderRequestDTO;
import com.wrt.excel.order.entity.Dish;
import com.wrt.excel.order.entity.Order;
import com.wrt.excel.order.entity.OrderItem;
import com.wrt.excel.order.mapper.DishMapper;
import com.wrt.excel.order.mapper.OrderItemMapper;
import com.wrt.excel.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 订单服务
 */
@Slf4j
@Service
public class OrderService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private OrderItemMapper orderItemMapper;
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private CoinService coinService;
    
    @Autowired
    private AchievementService achievementService;
    
    @Autowired
    private ServerChanService serverChanService;

    @Autowired
    private WechatMpService wechatMpService;

    @Autowired
    private AppNotifyService appNotifyService;
    
    /**
     * 创建订单
     */
    private static final List<String> CURRENCY_ORDER = List.of("爱心币", "亲亲", "抱抱");

    /** 规范化币种：空/未知按爱心币；抱抱/亲亲等软币种不折算、不扣爱心币余额 */
    public static String normalizePriceType(String raw) {
        if (raw == null || raw.isBlank()) return "爱心币";
        String pt = raw.trim();
        if ("多币种".equals(pt)) return pt;
        if (pt.contains("抱抱") || "hug".equalsIgnoreCase(pt)) return "抱抱";
        if (pt.contains("亲亲") || "kiss".equalsIgnoreCase(pt)) return "亲亲";
        if (pt.contains("爱心") || "coin".equalsIgnoreCase(pt) || "love".equalsIgnoreCase(pt)) return "爱心币";
        return pt;
    }

    public static boolean isLoveCoin(String priceType) {
        return "爱心币".equals(normalizePriceType(priceType));
    }

    /** 展示用分币种合计，币种之间用「 + 」分开，不做汇率折算 */
    public static String formatPriceSummary(Map<String, Integer> totalsByType) {
        if (totalsByType == null || totalsByType.isEmpty()) return "0 爱心币";
        List<String> keys = new ArrayList<>(totalsByType.keySet());
        keys.sort((a, b) -> {
            int ia = CURRENCY_ORDER.indexOf(a);
            int ib = CURRENCY_ORDER.indexOf(b);
            if (ia < 0) ia = 99;
            if (ib < 0) ib = 99;
            if (ia != ib) return Integer.compare(ia, ib);
            return a.compareTo(b);
        });
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            Integer value = totalsByType.get(key);
            if (value == null || value <= 0) continue;
            if (sb.length() > 0) sb.append(" + ");
            sb.append(value).append(' ').append(key);
        }
        return sb.length() == 0 ? "0 爱心币" : sb.toString();
    }

    /** 落库用短标签，避免把长摘要写进 varchar(20) 的 price_type */
    private static String shortPriceType(Map<String, Integer> totalsByType) {
        List<String> positive = new ArrayList<>();
        if (totalsByType != null) {
            for (Map.Entry<String, Integer> e : totalsByType.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) positive.add(e.getKey());
            }
        }
        if (positive.size() == 1) return positive.get(0);
        if (positive.isEmpty()) return "爱心币";
        return "多币种";
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(OrderRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("购物车为空，无法下单");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("用户未登录");
        }

        String orderNo = generateOrderNo();

        // 按币种分别累计；仅爱心币走余额扣款，禁止把抱抱/亲亲当爱心币累加扣减
        java.util.Map<String, Integer> totalsByType = new java.util.LinkedHashMap<>();
        java.util.List<Dish> validatedDishes = new java.util.ArrayList<>();

        for (CartItemDTO item : request.getItems()) {
            if (item.getDishId() == null) {
                throw new IllegalArgumentException("菜品ID无效");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("菜品数量必须大于0");
            }
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish == null) {
                throw new IllegalArgumentException("菜品不存在: " + item.getDishId());
            }
            if (dish.getIsSoldOut() != null && dish.getIsSoldOut() == 1) {
                throw new IllegalArgumentException("菜品已售罄: " + dish.getName());
            }
            int unit = dish.getPrice() == null ? 0 : dish.getPrice();
            if (unit < 0) {
                throw new IllegalArgumentException("菜品价格无效: " + dish.getName());
            }
            String pt = normalizePriceType(dish.getPriceType());
            totalsByType.merge(pt, unit * item.getQuantity(), Integer::sum);
            validatedDishes.add(dish);
        }

        int loveTotal = totalsByType.getOrDefault("爱心币", 0);
        int softTotal = totalsByType.entrySet().stream()
                .filter(e -> !"爱心币".equals(e.getKey()))
                .mapToInt(e -> e.getValue() == null ? 0 : e.getValue())
                .sum();
        if (loveTotal <= 0 && softTotal <= 0) {
            throw new IllegalArgumentException("订单金额无效");
        }

        String priceSummary = formatPriceSummary(totalsByType);

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        // totalPrice 只记爱心币，取消时按此退回；亲亲/抱抱不折算进爱心币、也不扣余额
        order.setTotalPrice(loveTotal);
        order.setPriceType(shortPriceType(totalsByType));
        order.setPriceSummary(priceSummary);
        order.setCurrencyTotals(new LinkedHashMap<>(totalsByType));
        order.setStatus("pending");
        order.setRemark(request.getRemark());
        order.setCreatedAt(LocalDateTime.now());

        orderMapper.insert(order);

        if (loveTotal > 0) {
            coinService.useCoins(request.getUserId(), loveTotal, "use_order",
                    String.format("点单消费(爱心币): %s", orderNo), order.getId());
        }
        for (Map.Entry<String, Integer> entry : totalsByType.entrySet()) {
            if ("爱心币".equals(entry.getKey()) || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            log.info("订单 {} 记录软币种应付 {} {}（不扣爱心币）", orderNo, entry.getValue(), entry.getKey());
        }
        if (loveTotal <= 0) {
            log.info("订单 {} 无爱心币项，跳过扣币（软币种合计 {}）", orderNo, softTotal);
        }

        for (int i = 0; i < request.getItems().size(); i++) {
            CartItemDTO item = request.getItems().get(i);
            Dish dish = validatedDishes.get(i);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setDishId(dish.getId());
            orderItem.setDishName(dish.getName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(dish.getPrice());
            String itemRemark = item.getRemark() == null ? "" : item.getRemark();
            String pt = normalizePriceType(dish.getPriceType());
            if (!itemRemark.contains("[币种:")) {
                itemRemark = (itemRemark.isBlank() ? "" : itemRemark + " ") + "[币种:" + pt + "]";
            }
            orderItem.setRemark(itemRemark.trim());
            orderItem.setCreatedAt(LocalDateTime.now());

            orderItemMapper.insert(orderItem);
        }

        log.info("订单创建成功，订单号: {}", orderNo);
        
        // 6. 检查并解锁成就（失败不影响订单）
        try {
            achievementService.checkAndUnlockAchievements(request.getUserId());
        } catch (Exception e) {
            log.error("成就检查失败，但不影响订单创建: {}", e.getMessage());
        }
        
        // 7. 发送Server酱通知（失败不影响订单）
        try {
            sendOrderNotification(order, request.getItems());
        } catch (Exception e) {
            log.error("发送订单通知失败，但不影响订单创建: {}", e.getMessage());
        }
        
        return order;
    }
    
    /**
     * 获取用户订单列表
     */
    public List<Order> getUserOrders(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .orderByDesc(Order::getCreatedAt);
        List<Order> orders = orderMapper.selectList(wrapper);
        enrichPriceSummaries(orders);
        return orders;
    }
    
    /**
     * 获取所有订单列表（后台管理用）
     */
    public List<Order> getAllOrders() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getCreatedAt);
        List<Order> orders = orderMapper.selectList(wrapper);
        enrichPriceSummaries(orders);
        return orders;
    }
    
    /**
     * 根据订单号获取订单
     */
    public Order getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        return orderMapper.selectOne(wrapper);
    }
    
    /**
     * 根据ID获取订单
     */
    public Order getOrderById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            enrichPriceSummaries(List.of(order));
        }
        return order;
    }

    /**
     * 按明细上的 [币种:xx] 重算分币种合计，避免把亲亲/抱抱折进爱心币展示。
     */
    public void enrichPriceSummaries(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> ids = new ArrayList<>();
        for (Order order : orders) {
            if (order != null && order.getId() != null) ids.add(order.getId());
        }
        Map<Long, List<OrderItem>> grouped = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(OrderItem::getOrderId, ids);
            List<OrderItem> items = orderItemMapper.selectList(wrapper);
            if (items != null) {
                for (OrderItem item : items) {
                    grouped.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
                }
            }
        }
        for (Order order : orders) {
            if (order == null) continue;
            applyPriceSummary(order, grouped.get(order.getId()));
        }
    }

    private void applyPriceSummary(Order order, List<OrderItem> items) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        if (items != null) {
            for (OrderItem item : items) {
                String pt = currencyFromItem(item, order);
                int qty = item.getQuantity() == null ? 1 : item.getQuantity();
                int price = item.getPrice() == null ? 0 : item.getPrice();
                if (qty > 0 && price > 0) {
                    totals.merge(pt, price * qty, Integer::sum);
                }
            }
        }
        if (totals.isEmpty()) {
            String pt = normalizePriceType(order.getPriceType());
            int amount = order.getTotalPrice() == null ? 0 : order.getTotalPrice();
            if ("多币种".equals(pt)) pt = "爱心币";
            if (amount > 0) totals.put(pt, amount);
        }
        order.setCurrencyTotals(totals);
        order.setPriceSummary(formatPriceSummary(totals));
    }

    private String currencyFromItem(OrderItem item, Order order) {
        String remark = item.getRemark();
        if (remark != null) {
            int start = remark.indexOf("[币种:");
            if (start >= 0) {
                int end = remark.indexOf(']', start);
                if (end > start) {
                    return normalizePriceType(remark.substring(start + 4, end));
                }
            }
        }
        String pt = normalizePriceType(order == null ? null : order.getPriceType());
        return "多币种".equals(pt) ? "爱心币" : pt;
    }
    
    /**
     * 获取订单明细
     */
    public List<OrderItem> getOrderItems(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }
    
    /**
     * 更新订单状态
     */
        /** 简化状态机：pending→cooking→ready→completed；旧状态兼容流转（取消走 cancelOrder） */
    private static final java.util.Map<String, java.util.Set<String>> STATUS_TRANSITIONS = java.util.Map.of(
            "pending", java.util.Set.of("cooking"),
            "confirmed", java.util.Set.of("cooking"),
            "received", java.util.Set.of("cooking"),
            "preparing", java.util.Set.of("ready"),
            "cooking", java.util.Set.of("ready"),
            "ready", java.util.Set.of("completed"),
            "delivered", java.util.Set.of("completed")
    );

    public void updateOrderStatus(String orderNo, String status) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("状态不能为空");
        }
        status = status.trim().toLowerCase();
        if ("cancelled".equals(status) || "canceled".equals(status)) {
            throw new IllegalArgumentException("取消订单请使用取消接口");
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(wrapper);
        
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        String oldStatus = order.getStatus() == null ? "" : order.getStatus().trim().toLowerCase();
        if (oldStatus.equals(status)) {
            return;
        }
        if ("cancelled".equals(oldStatus) || "canceled".equals(oldStatus) || "completed".equals(oldStatus)) {
            throw new IllegalArgumentException("订单已结束，无法变更状态");
        }
        java.util.Set<String> allowed = STATUS_TRANSITIONS.get(oldStatus);
        if (allowed == null || !allowed.contains(status)) {
            throw new IllegalArgumentException("非法状态流转: " + oldStatus + " -> " + status);
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单状态更新成功，订单号: {}, 状态: {} -> {}", orderNo, oldStatus, status);
        
        try {
            serverChanService.sendOrderStatusChangeNotification(orderNo, oldStatus, status);
        } catch (Exception e) {
            log.error("发送订单状态变更通知失败，但不影响订单状态更新: {}", e.getMessage());
        }
        // Always template 点单方 openid (WechatMpService.notifyStatusChange);
        // cooking = 接单制作, ready = 做好了
        String wxRemark;
        if ("cooking".equals(status)) {
            wxRemark = "接单制作中";
        } else if ("ready".equals(status)) {
            wxRemark = "做好了，可以取餐";
        } else if ("completed".equals(status)) {
            wxRemark = "订单已完成";
        } else {
            wxRemark = "状态更新";
        }
        try {
            wechatMpService.notifyStatusChange(
                    orderNo,
                    status,
                    buildDishSummary(order.getId()),
                    wxRemark,
                    false
            );
        } catch (Exception e) {
            log.error("发送微信状态通知失败，但不影响订单状态更新: {}", e.getMessage());
        }
        try {
            appNotifyService.notifyStatusChange(
                    order.getUserId(),
                    orderNo,
                    status,
                    buildDishSummary(order.getId()),
                    wxRemark
            );
        } catch (Exception e) {
            log.error("发送App状态通知失败，但不影响订单状态更新: {}", e.getMessage());
        }
    }
    
    /**
     * 取消订单（仅pending状态可取消）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(wrapper);
        
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        
        // 验证用户权限
        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("无权操作此订单");
        }
        
        // 仅待接单可取消（含旧状态 confirmed/received）
        String cur = order.getStatus() == null ? "" : order.getStatus().trim().toLowerCase();
        if (!("pending".equals(cur) || "confirmed".equals(cur) || "received".equals(cur))) {
            throw new IllegalStateException("订单已接单，无法取消。如需帮助请联系管理员");
        }
        
        // 更新订单状态为cancelled，再退币；退币失败则整单回滚，避免“已取消未退款”
        String oldStatus = order.getStatus();
        order.setStatus("cancelled");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        
        log.info("订单取消成功，订单号: {}, 用户ID: {}", orderNo, userId);
        
        Integer refundAmount = order.getTotalPrice();
        if (refundAmount != null && refundAmount > 0) {
            coinService.refundCoins(userId, refundAmount, "refund_order",
                    String.format("订单取消退款: %s", orderNo), order.getId());
            log.info("订单取消，爱心币已退还: {}", orderNo);
        }
        
        try {
            serverChanService.sendOrderStatusChangeNotification(orderNo, oldStatus, "cancelled");
        } catch (Exception e) {
            log.error("发送订单取消通知失败: {}", e.getMessage());
        }
        try {
            wechatMpService.notifyStatusChange(
                    orderNo,
                    "cancelled",
                    buildDishSummary(order.getId()),
                    "订单已取消",
                    true
            );
        } catch (Exception e) {
            log.error("发送微信取消通知失败: {}", e.getMessage());
        }
        try {
            appNotifyService.notifyCancel(
                    order.getUserId(),
                    orderNo,
                    buildDishSummary(order.getId())
            );
        } catch (Exception e) {
            log.error("发送App取消通知失败: {}", e.getMessage());
        }
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8).replace("-", "");
        return "LOVE" + timestamp + random;
    }
    
    /**
     * 发送订单通知
     */
    private void sendOrderNotification(Order order, List<CartItemDTO> items) {
        StringBuilder title = new StringBuilder();
        title.append("🛒 新订单提醒: ").append(order.getOrderNo());
        
        StringBuilder description = new StringBuilder();
        description.append("## 💕 新订单通知\n\n");
        description.append("**订单号**: ").append(order.getOrderNo()).append("\n\n");
        description.append("**总价**: ").append(displayPrice(order)).append("\n\n");
        
        if (order.getRemark() != null && !order.getRemark().isEmpty()) {
            description.append("**备注**: ").append(order.getRemark()).append("\n\n");
        }
        
        description.append("### 📋 订单明细:\n\n");
        for (int i = 0; i < items.size(); i++) {
            CartItemDTO item = items.get(i);
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish != null) {
                description.append((i + 1)).append(". **").append(dish.getName()).append("**");
                description.append(" x").append(item.getQuantity());
                if (item.getRemark() != null && !item.getRemark().isEmpty()) {
                    description.append(" (").append(item.getRemark()).append(")");
                }
                description.append("\n");
            }
        }
        
        description.append("\n> ⏰ 请及时处理订单哦！");
        
        serverChanService.sendNotification(title.toString(), description.toString());

        // WeChat template to chef (failures already swallowed inside service)
        StringBuilder dishSummary = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            CartItemDTO item = items.get(i);
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish != null) {
                if (dishSummary.length() > 0) dishSummary.append("、");
                dishSummary.append(dish.getName()).append("x").append(item.getQuantity());
            }
        }
        String priceText = displayPrice(order);
        String userLabel = "点单方";
        try {
            // nickname optional; keep label simple
            userLabel = "宝贝";
        } catch (Exception ignored) {}
        wechatMpService.notifyNewOrder(
                order.getOrderNo(),
                userLabel,
                dishSummary.toString(),
                priceText,
                order.getRemark()
        );
        try {
            appNotifyService.notifyNewOrder(
                    order.getUserId(),
                    order.getOrderNo(),
                    dishSummary.toString(),
                    priceText,
                    order.getRemark()
            );
        } catch (Exception e) {
            log.error("发送App新订单通知失败: {}", e.getMessage());
        }
    }

    private static String displayPrice(Order order) {
        if (order.getPriceSummary() != null && !order.getPriceSummary().isBlank()) {
            return order.getPriceSummary();
        }
        String pt = order.getPriceType() == null ? "爱心币" : order.getPriceType();
        return (order.getTotalPrice() == null ? 0 : order.getTotalPrice()) + " " + pt;
    }

    private String buildDishSummary(Long orderId) {
        try {
            java.util.List<OrderItem> items = getOrderItems(orderId);
            if (items == null || items.isEmpty()) return "-";
            StringBuilder sb = new StringBuilder();
            for (OrderItem it : items) {
                if (sb.length() > 0) sb.append("、");
                sb.append(it.getDishName() == null ? "?" : it.getDishName())
                        .append("x").append(it.getQuantity() == null ? 1 : it.getQuantity());
            }
            return sb.toString();
        } catch (Exception e) {
            return "-";
        }
    }
}
