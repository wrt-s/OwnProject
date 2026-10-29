package com.wrt.excel.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ServerChanServiceTest {

    @Autowired
    private ServerChanService serverChanService;

    @Test
    public void testSendOrderStatusChangeNotification() {
        // 测试接单通知: pending -> received
        boolean result1 = serverChanService.sendOrderStatusChangeNotification("LOVE20260515001", "pending", "received");
        System.out.println("接单通知发送结果: " + result1);
        
        // 测试开始制作通知: received -> cooking
        boolean result2 = serverChanService.sendOrderStatusChangeNotification("LOVE20260515002", "received", "cooking");
        System.out.println("开始制作通知发送结果: " + result2);
        
        // 测试已送达通知: cooking -> delivered
        boolean result3 = serverChanService.sendOrderStatusChangeNotification("LOVE20260515003", "cooking", "delivered");
        System.out.println("已送达通知发送结果: " + result3);
        
        // 测试已完成通知: delivered -> completed
        boolean result4 = serverChanService.sendOrderStatusChangeNotification("LOVE20260515004", "delivered", "completed");
        System.out.println("已完成通知发送结果: " + result4);
        
        // 测试取消订单通知
        boolean result5 = serverChanService.sendOrderStatusChangeNotification("LOVE20260515005", "pending", "cancelled");
        System.out.println("取消订单通知发送结果: " + result5);
        
        // 测试其他状态变更通知
        boolean result6 = serverChanService.sendOrderStatusChangeNotification("LOVE20260515006", "unknown", "custom_status");
        System.out.println("其他状态变更通知发送结果: " + result6);
    }
}