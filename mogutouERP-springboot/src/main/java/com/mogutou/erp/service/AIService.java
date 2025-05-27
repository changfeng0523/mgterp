package com.mogutou.erp.service;

import com.mogutou.erp.dto.AIRequest;
import com.mogutou.erp.dto.AIResponse;

/**
 * AI服务接口
 */
public interface AIService {
    
    /**
     * 解析自然语言并执行相应操作
     */
    AIResponse parseAndExecute(String input, boolean confirmed);
    
    /**
     * 获取业务洞察分析
     */
    AIResponse getBusinessInsights(AIRequest request);
} 