package com.mogutou.erp.controller;

import com.mogutou.erp.service.AIService;
import com.mogutou.erp.service.external.DeepSeekAIService;
import com.mogutou.erp.dto.AIRequest;
import com.mogutou.erp.dto.AIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * AI控制器
 * 处理自然语言理解和AI分析相关的HTTP请求
 */
@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private AIService aiService;
    
    @Autowired
    private DeepSeekAIService deepSeekAIService;

    /**
     * 解析自然语言并执行相应操作
     */
    @PostMapping("/parse")
    public AIResponse parse(@RequestBody AIRequest request) {
        return aiService.parseAndExecute(request.getInput(), request.isConfirmed());
    }

    /**
     * 获取业务洞察分析
     */
    @PostMapping("/insights")
    public AIResponse getInsights(@RequestBody AIRequest request) {
        return aiService.getBusinessInsights(request);
    }

    /**
     * AI服务状态检查
     */
    @GetMapping("/status")
    public Map<String, Object> getAIStatus() {
        return deepSeekAIService.getServiceStatus();
    }

    /**
     * AI服务健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        boolean healthy = deepSeekAIService.healthCheck();
        return Map.of(
            "healthy", healthy,
            "status", healthy ? "OK" : "ERROR",
            "timestamp", System.currentTimeMillis()
        );
    }
} 