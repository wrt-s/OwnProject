package com.wrt.excel;

import com.google.common.util.concurrent.RateLimiter;
import com.wrt.excel.system.service.ProvincialApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("省平台API服务单元测试")
class ProvincialApiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private ProvincialApiService provincialApiService;

    private Object testParams;

    @BeforeEach
    void setUp() {
        testParams = new Object();
    }

    @Test
    @DisplayName("测试手动限流 - 成功获取令牌")
    void testUploadReport1_Success() throws Exception {

        // 准备测试数据
        String expectedResult = "success";

        // Mock限流器行为
        when(rateLimiter.tryAcquire()).thenReturn(true);

        // Mock RestTemplate行为
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn(expectedResult);

        // 执行测试
        String result = provincialApiService.uploadReport1(testParams);

        // 验证结果
        assertNotNull(result);
        assertEquals(expectedResult, result);

        // 验证方法调用
        verify(rateLimiter, times(1)).tryAcquire();
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("测试手动限流 - 限流失败")
    void testUploadReport1_RateLimitExceeded() {
        // Mock限流器行为 - 获取令牌失败
        when(rateLimiter.tryAcquire()).thenReturn(false);

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            provincialApiService.uploadReport1(testParams);
        });

        assertEquals("请求过于频繁，请稍后重试", exception.getMessage());

        // 验证RestTemplate没有被调用
        verify(restTemplate, never()).postForObject(anyString(), any(), eq(String.class));
    }


    @Test
    @DisplayName("测试注解限流 - 成功")
    void testUploadReport3_Success() {
        // 准备测试数据
        String expectedResult = "success";

        // Mock RestTemplate
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn(expectedResult);

        // 执行测试
        String result = provincialApiService.uploadReport3(testParams);

        // 验证结果
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    @DisplayName("测试用户级限流")
    void testUploadReportByUser_Success() {
        // 准备测试数据
        String userId = "user123";
        String expectedResult = "success";

        // Mock RestTemplate
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn(expectedResult);

        // 执行测试
        String result = provincialApiService.uploadReportByUser(userId, testParams);

        // 验证结果
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    @DisplayName("测试省平台接口调用失败")
    void testCallProvincialApi_Failure() {
        // Mock RestTemplate抛出异常
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            provincialApiService.uploadReport3(testParams);
        });

        assertEquals("调用省平台接口失败", exception.getMessage());
    }
}