package com.wrt.excel.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Server酱推送服务
 * 用于通过方糖公众号发送消息提醒
 */
@Slf4j
@Service
public class ServerChanService {
    
    @Value("${serverchan.key:}")
    private String serverChanKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 发送通知
     * @param title 标题
     * @param description 描述内容
     * @return 是否发送成功
     */
    public boolean sendNotification(String title, String description) {
        if (serverChanKey == null || serverChanKey.isEmpty()) {
            log.warn("Server酱配置未设置，无法发送通知");
            return false;
        }
        
        try {
            String url = "https://sctapi.ftqq.com/" + serverChanKey + ".send";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            Map<String, String> params = new HashMap<>();
            params.put("title", title);
            params.put("desp", description);
            
            StringBuilder formData = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (formData.length() > 0) {
                    formData.append("&");
                }
                formData.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            
            HttpEntity<String> entity = new HttpEntity<>(formData.toString(), headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Server酱通知发送成功: {}", title);
                return true;
            } else {
                log.error("Server酱通知发送失败，状态码: {}", response.getStatusCode());
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            log.error("编码错误", e);
            return false;
        } catch (Exception e) {
            log.error("发送Server酱通知异常", e);
            return false;
        }
    }
    
    /**
     * 发送待办提醒
     * @param todoTitle 待办标题
     * @param assignee 负责人
     * @param dueDate 截止日期
     * @param priority 优先级
     * @return 是否发送成功
     */
    public boolean sendTodoReminder(String todoTitle, String assignee, String dueDate, String priority) {
        String title = "📋 待办提醒: " + todoTitle;
        
        StringBuilder description = new StringBuilder();
        description.append("## 待办事项提醒\n\n");
        description.append("**标题**: ").append(todoTitle).append("\n\n");
        
        if (assignee != null && !assignee.isEmpty()) {
            description.append("**负责人**: ").append(assignee).append("\n\n");
        }
        
        if (dueDate != null && !dueDate.isEmpty()) {
            description.append("**截止日期**: ").append(dueDate).append("\n\n");
        }
        
        if (priority != null && !priority.isEmpty()) {
            String priorityText = getPriorityText(priority);
            description.append("**优先级**: ").append(priorityText).append("\n\n");
        }
        
        description.append("> ⏰ 请及时处理哦！");
        
        return sendNotification(title, description.toString());
    }
    
    /**
     * 发送订单状态变更通知
     * @param orderNo 订单号
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @return 是否发送成功
     */
    public boolean sendOrderStatusChangeNotification(String orderNo, String oldStatus, String newStatus) {
        // 根据状态变更生成不同的标题和描述
        String title = generateTitleByStatusChange(oldStatus, newStatus, orderNo);
        String description = generateDescriptionByStatusChange(orderNo, oldStatus, newStatus);
        
        return sendNotification(title, description);
    }
    
    /**
     * 根据状态变更生成标题
     */
    private String generateTitleByStatusChange(String oldStatus, String newStatus, String orderNo) {
        if (oldStatus == null) oldStatus = "";
        
        // 接单: pending -> received
        if ("pending".equalsIgnoreCase(oldStatus) && "received".equalsIgnoreCase(newStatus)) {
            return "✅ 订单已接单: " + orderNo;
        }
        // 开始制作: received -> cooking
        else if ("received".equalsIgnoreCase(oldStatus) && "cooking".equalsIgnoreCase(newStatus)) {
            return "👨‍🍳 订单开始制作: " + orderNo;
        }
        // 已送达: cooking -> delivered
        else if ("cooking".equalsIgnoreCase(oldStatus) && "delivered".equalsIgnoreCase(newStatus)) {
            return "🚚 订单已送达: " + orderNo;
        }
        // 已完成: delivered -> completed
        else if ("delivered".equalsIgnoreCase(oldStatus) && "completed".equalsIgnoreCase(newStatus)) {
            return "🎉 订单已完成: " + orderNo;
        }
        // 取消订单
        else if ("cancelled".equalsIgnoreCase(newStatus)) {
            return "❌ 订单已取消: " + orderNo;
        }
        // 其他状态变更
        else {
            return "📦 订单状态更新: " + orderNo;
        }
    }
    
    /**
     * 根据状态变更生成详细描述
     */
    private String generateDescriptionByStatusChange(String orderNo, String oldStatus, String newStatus) {
        StringBuilder description = new StringBuilder();
        String newStatusText = getStatusText(newStatus);
        String oldStatusText = oldStatus != null ? getStatusText(oldStatus) : "未知";
        String updateTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        // 接单: pending -> received
        if ("pending".equalsIgnoreCase(oldStatus) && "received".equalsIgnoreCase(newStatus)) {
            description.append("## ✅ 订单已接单\n\n");
            description.append("**订单号**: ").append(orderNo).append("\n\n");
            description.append("**状态变更**: ⏳ 待处理 → ✅ 已接单\n\n");
            description.append("**接单时间**: ").append(updateTime).append("\n\n");
            description.append("> 👨‍🍳 厨师已开始准备您的订单，请耐心等待！");
        }
        // 开始制作: received -> cooking
        else if ("received".equalsIgnoreCase(oldStatus) && "cooking".equalsIgnoreCase(newStatus)) {
            description.append("## 👨‍🍳 订单开始制作\n\n");
            description.append("**订单号**: ").append(orderNo).append("\n\n");
            description.append("**状态变更**: ✅ 已接单 → 👨‍🍳 制作中\n\n");
            description.append("**开始制作时间**: ").append(updateTime).append("\n\n");
            description.append("> 🔥 您的订单正在火热制作中，马上就好！");
        }
        // 已送达: cooking -> delivered
        else if ("cooking".equalsIgnoreCase(oldStatus) && "delivered".equalsIgnoreCase(newStatus)) {
            description.append("## 🚚 订单已送达\n\n");
            description.append("**订单号**: ").append(orderNo).append("\n\n");
            description.append("**状态变更**: 👨‍🍳 制作中 → 🚚 已送达\n\n");
            description.append("**送达时间**: ").append(updateTime).append("\n\n");
            description.append("> 📬 您的订单已送达，请及时取餐！");
        }
        // 已完成: delivered -> completed
        else if ("delivered".equalsIgnoreCase(oldStatus) && "completed".equalsIgnoreCase(newStatus)) {
            description.append("## 🎉 订单已完成\n\n");
            description.append("**订单号**: ").append(orderNo).append("\n\n");
            description.append("**状态变更**: 🚚 已送达 → 🎉 已完成\n\n");
            description.append("**完成时间**: ").append(updateTime).append("\n\n");
            description.append("> 💕 感谢您的订购，期待下次为您服务！");
        }
        // 取消订单
        else if ("cancelled".equalsIgnoreCase(newStatus)) {
            description.append("## ❌ 订单已取消\n\n");
            description.append("**订单号**: ").append(orderNo).append("\n\n");
            if (oldStatus != null && !oldStatus.isEmpty()) {
                description.append("**状态变更**: ").append(oldStatusText).append(" → ❌ 已取消\n\n");
            }
            description.append("**取消时间**: ").append(updateTime).append("\n\n");
            description.append("> ℹ️ 订单已取消，如有需要请重新下单。");
        }
        // 其他状态变更（通用格式）
        else {
            description.append("## 📦 订单状态变更通知\n\n");
            description.append("**订单号**: ").append(orderNo).append("\n\n");
            description.append("**新状态**: ").append(newStatusText).append("\n\n");
            
            if (oldStatus != null && !oldStatus.isEmpty()) {
                description.append("**原状态**: ").append(oldStatusText).append("\n\n");
            }
            
            description.append("**更新时间**: ").append(updateTime).append("\n\n");
            description.append("> 💕 订单状态已更新，请及时关注！");
        }
        
        return description.toString();
    }
    
    /**
     * 获取状态文本
     */
    private String getStatusText(String status) {
        switch (status.toLowerCase()) {
            case "pending":
                return "⏳ 待处理";
            case "received":
                return "✅ 已接单";
            case "cooking":
                return "👨‍🍳 制作中";
            case "delivered":
                return "🚚 已送达";
            case "completed":
                return "🎉 已完成";
            case "cancelled":
                return "❌ 已取消";
            default:
                return status;
        }
    }
    
    /**
     * 获取优先级文本
     */
    private String getPriorityText(String priority) {
        switch (priority.toLowerCase()) {
            case "high":
                return "🔴 高";
            case "medium":
                return "🟡 中";
            case "low":
                return "🟢 低";
            default:
                return priority;
        }
    }
}
