package com.mogutou.erp.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 优化的DeepSeek AI服务
 * 支持智能对话、指令解析、业务分析等多种模式
 */
@Service
public class DeepSeekAIService {

    private static final String API_KEY = "sk-633c1a70b16c42cbb8b02bba706ac495";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    
    // 不同场景的超时配置
    private static final int INTENT_TIMEOUT = 15; // 意图识别：快速响应
    private static final int COMMAND_TIMEOUT = 20; // 指令解析：中等响应
    private static final int CONVERSATION_TIMEOUT = 25; // 对话交流：较长响应
    private static final int ANALYSIS_TIMEOUT = 45; // 业务分析：最长响应
    private static final int ORDER_ANALYSIS_TIMEOUT = 60; // 订单分析：超长响应

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 智能对话模式 - 自然语言交流
     */
    public String chat(String message) throws IOException {
        String systemPrompt = buildChatPrompt();
        return callAIWithRetry(message, systemPrompt, CONVERSATION_TIMEOUT, "CHAT");
    }

    /**
     * 意图识别模式 - 快速判断用户意图
     */
    public String analyzeIntent(String input) throws IOException {
        String systemPrompt = buildIntentPrompt();
        return callAIWithRetry(input, systemPrompt, INTENT_TIMEOUT, "INTENT");
    }

    /**
     * 指令解析模式 - 转换为JSON指令
     */
    public String parseCommand(String input) throws IOException {
        String systemPrompt = buildCommandPrompt();
        return callAIWithRetry(input, systemPrompt, COMMAND_TIMEOUT, "COMMAND");
    }

    /**
     * 业务分析模式 - 深度数据分析
     */
    public String analyzeData(String data, String analysisType) throws IOException {
        String systemPrompt = buildAnalysisPrompt(analysisType);
        int timeout = "ORDER".equals(analysisType) ? ORDER_ANALYSIS_TIMEOUT : ANALYSIS_TIMEOUT;
        return callAIWithRetry(data, systemPrompt, timeout, "ANALYSIS");
    }

    /**
     * 快速订单分析模式 - 优化超时处理
     */
    public String analyzeOrderData(String data) throws IOException {
        String systemPrompt = buildOrderAnalysisPrompt();
        return callAIWithRetry(data, systemPrompt, ORDER_ANALYSIS_TIMEOUT, "ORDER_ANALYSIS");
    }

    /**
     * 自定义提示词模式 - 灵活调用
     */
    public String askWithCustomPrompt(String input, String systemPrompt) throws IOException {
        return callAIWithRetry(input, systemPrompt, CONVERSATION_TIMEOUT, "CUSTOM");
    }

    /**
     * 带重试机制的AI调用
     */
    private String callAIWithRetry(String input, String systemPrompt, int timeoutSeconds, String mode) throws IOException {
        int maxRetries = 3;
        long baseDelay = 1000; // 1秒基础延迟
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println(String.format("🤖 AI调用[%s] - 尝试%d/%d", mode, attempt, maxRetries));
                return callDeepSeekAPI(input, systemPrompt, timeoutSeconds);
                
            } catch (IOException e) {
                System.err.println(String.format("❌ AI调用失败[%s] - 尝试%d: %s", mode, attempt, e.getMessage()));
                
                if (attempt == maxRetries) {
                    throw new IOException(String.format("AI服务调用失败，已重试%d次：%s", maxRetries, e.getMessage()));
                }
                
                // 指数退避延迟
                try {
                    long delay = baseDelay * (1L << (attempt - 1)); // 1s, 2s, 4s
                    System.out.println(String.format("⏳ 等待%dms后重试...", delay));
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("重试被中断", ie);
                }
            }
        }
        throw new IOException("不应该到达这里");
    }

    /**
     * 核心AI API调用方法
     */
    private String callDeepSeekAPI(String input, String systemPrompt, int timeoutSeconds) throws IOException {
        OkHttpClient client = buildHttpClient(timeoutSeconds);
        
        Map<String, Object> payload = buildRequestPayload(input, systemPrompt);
        String requestBody = mapper.writeValueAsString(payload);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "MogutouERP/1.0")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorDetail = response.body() != null ? response.body().string() : "无详细错误信息";
                throw new IOException(String.format("API请求失败 [%d]: %s - %s", 
                    response.code(), response.message(), errorDetail));
            }

            String responseBody = response.body().string();
            return parseAIResponse(responseBody);
        }
    }

    /**
     * 构建HTTP客户端
     */
    private OkHttpClient buildHttpClient(int timeoutSeconds) {
        return new OkHttpClient.Builder()
                .connectTimeout(Math.min(timeoutSeconds / 2, 10), TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds + 5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * 构建请求负载
     */
    private Map<String, Object> buildRequestPayload(String input, String systemPrompt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "deepseek-chat");
        payload.put("temperature", 0.7); // 适中的创造性
        payload.put("max_tokens", 2000); // 限制响应长度
        payload.put("top_p", 0.9);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", input));
        payload.put("messages", messages);

        return payload;
    }

    /**
     * 解析AI响应
     */
    private String parseAIResponse(String responseBody) throws IOException {
        try {
            JsonNode root = mapper.readTree(responseBody);
            
            // 检查错误
            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText("未知错误");
                throw new IOException("AI服务返回错误: " + errorMsg);
            }
            
            // 提取回复内容
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                throw new IOException("AI响应中没有choices字段");
            }
            
            String content = choices.get(0).path("message").path("content").asText();
            if (content.isEmpty()) {
                throw new IOException("AI响应内容为空");
            }
            
            // 智能内容清理
            return cleanAIResponse(content);
            
        } catch (Exception e) {
            System.err.println("📄 AI原始响应: " + responseBody);
            throw new IOException("解析AI响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 智能清理AI响应内容
     */
    private String cleanAIResponse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        String cleaned = content.trim();
        
        // 移除markdown代码块标记
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastTripleBacktick = cleaned.lastIndexOf("```");
            if (firstNewline != -1 && lastTripleBacktick > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastTripleBacktick).trim();
            }
        }
        
        // 如果是JSON格式，验证并格式化
        if (isJSONContent(cleaned)) {
            try {
                JsonNode json = mapper.readTree(cleaned);
                return mapper.writeValueAsString(json); // 标准化JSON格式
            } catch (Exception e) {
                System.out.println("⚠️ JSON格式化失败，返回原内容: " + e.getMessage());
            }
        }
        
        return cleaned;
    }

    /**
     * 判断内容是否为JSON格式
     */
    private boolean isJSONContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    /**
     * 构建对话提示词
     */
    private String buildChatPrompt() {
        return """
            你是蘑菇头ERP系统的AI助手，名字叫小蘑菇🍄。你的性格特点：
            
            🎯 核心特质:
            - 友好温馨，像贴心的小伙伴一样
            - 幽默风趣，但保持专业分寸
            - 主动帮助，善于理解用户真实需求
            - 简洁明了，避免冗长说教
            - 善于倾听，对用户的困难表示理解
            
            💼 专业能力:
            - ERP系统使用指导和最佳实践
            - 企业管理建议和流程优化
            - 业务数据分析和洞察解读
            - 供应链管理和库存优化
            - 财务管理和成本控制
            - 订单管理和客户关系
            
            💬 对话风格:
            - 🔥 热情主动: 主动询问需要什么帮助，积极提供解决方案
            - 😊 亲切自然: 使用温暖的语言，让用户感到轻松舒适
            - 🎯 精准高效: 快速理解问题核心，给出针对性建议
            - 💡 启发思考: 不只给答案，还解释原因和最佳实践
            - 🤝 感同身受: 理解用户的业务压力和挑战
            
            📚 智能响应策略:
            • 业务咨询: 提供实用的管理建议和操作指导
            • 技术支持: 解释系统功能，指导正确使用方法
            • 数据分析: 帮助解读报表，发现业务洞察
            • 流程优化: 建议改进业务流程，提高效率
            • 问题解决: 协助排查问题，提供多种解决方案
            
            🌟 特殊场景处理:
            - 订单管理问题：耐心指导，提供最佳实践
            - 系统操作疑惑：详细解释，避免用户迷茫
            - 业务流程困惑：分步骤指导，确保理解
            - 数据异常情况：冷静分析，给出排查方向
            - 紧急业务需求：快速响应，优先解决
            
            🎨 回复格式:
            - 适当使用emoji增加亲和力 (不要过多)
            - 重要信息直接表达，不要用星号粗体标记
            - 步骤用数字或bullet points清晰展示
            - 根据问题复杂度调整回复长度
            - 结尾主动询问是否需要更多帮助
            - 不要使用markdown格式如**粗体**等
            
            💝 温馨提醒:
            - 始终站在用户角度思考问题
            - 对不确定的信息要诚实说明
            - 遇到复杂问题时，建议分步骤处理
            - 保持耐心，即使是重复性问题
            - 记住你是用户可信赖的业务伙伴
            - 回复使用纯文本格式，避免markdown标记
            
            请用温暖专业的语调与用户交流，让每一次对话都成为愉快的体验！🌟
            """;
    }

    /**
     * 构建意图识别提示词
     */
    private String buildIntentPrompt() {
        return """
            你是智能意图识别专家。分析用户输入，判断其真实意图。
            
            🎯 **识别类型:**
            1. **COMMAND** - 要求执行具体系统操作
               - 关键词：创建、查询、删除、修改、统计、导出等
               - 示例：「创建订单」「查询销售额」「删除库存」
            
            2. **CONVERSATION** - 日常对话交流
               - 关键词：问候、感谢、询问、闲聊、求助等  
               - 示例：「你好」「谢谢」「今天天气」「你是谁」
            
            3. **MIXED** - 既有操作需求又有对话元素
               - 示例：「你好，帮我查一下订单」「麻烦创建个订单，谢谢」
            
            📊 **返回格式 (严格JSON):**
            {
              "intent_type": "COMMAND/CONVERSATION/MIXED",
              "confidence": 0.0-1.0,
              "command": "提取的核心操作指令(仅COMMAND/MIXED)",
              "reasoning": "判断依据(简短说明)"
            }
            
            🚨 **重要**: 只返回JSON，不要任何额外文字！
            """;
    }

    /**
     * 构建智能指令解析提示词
     */
    private String buildCommandPrompt() {
        return """
            你是智能ERP指令解析器。从用户输入中提取信息，转换为标准JSON。
            
            🎯 **解析规则（按优先级）:**
            1. 识别操作类型：创建→create_order，查询→query_order，删除→delete_order，分析→analyze_order
            2. 识别订单类型：采购关键词→PURCHASE，销售关键词→SALE，默认SALE
            3. 提取客户/供应商：匹配"为[姓名]"、"给[姓名]"、"从[姓名]"、"向[姓名]"等
            4. 提取商品：匹配商品名称+数量+价格的组合模式
            5. 智能推断缺失信息：缺价格设为0
            
            📦 **订单类型识别:**
            • **PURCHASE(采购)**: 采购、进货、购买、进料、补货、订购、从供应商、向厂家、从XX那里买
            • **SALE(销售)**: 销售、出售、卖给、售给、发货、交付、为客户、给客户
            
            📝 **解析示例（严格按此格式）:**
            
            ===== 🔵 销售订单示例 =====
            输入："创建订单"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "", "products": []}
            
            输入："为张三创建销售订单，苹果10个单价5元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "张三", "products": [{"name": "苹果", "quantity": 10, "unit_price": 5.0}]}
            
            输入："卖给李四20个橙子每个3元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "李四", "products": [{"name": "橙子", "quantity": 20, "unit_price": 3.0}]}
            
            输入："发货给王五，香蕉15个单价2元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "王五", "products": [{"name": "香蕉", "quantity": 15, "unit_price": 2.0}]}
            
            ===== 🟠 采购订单示例 =====
            输入："创建采购订单"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "", "products": []}
            
            输入："从供应商张三采购苹果100个单价3元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "张三", "products": [{"name": "苹果", "quantity": 100, "unit_price": 3.0}]}
            
            输入："向厂家进货橙子200个每个2.5元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "厂家", "products": [{"name": "橙子", "quantity": 200, "unit_price": 2.5}]}
            
            输入："从哈振宇那里买了5瓶水，一瓶3元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "哈振宇", "products": [{"name": "水", "quantity": 5, "unit_price": 3.0}]}
            
            输入："从李老板那里采购大米50袋单价80元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "李老板", "products": [{"name": "大米", "quantity": 50, "unit_price": 80.0}]}
            
            输入："购买原料，大米50袋单价80元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "", "products": [{"name": "大米", "quantity": 50, "unit_price": 80.0}]}
            
            输入："补货梨子30个价格4元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "", "products": [{"name": "梨子", "quantity": 30, "unit_price": 4.0}]}
            
            ===== 🆕 自然语言表达示例 =====
            输入："从王小明那里买10瓶饮料每瓶5块钱"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "王小明", "products": [{"name": "饮料", "quantity": 10, "unit_price": 5.0}]}
            
            输入："给客户刘大海发货，苹果20个一个3.5元"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "刘大海", "products": [{"name": "苹果", "quantity": 20, "unit_price": 3.5}]}
            
            输入："和张师傅订了30斤大米每斤6元"
            输出：{"action": "create_order", "order_type": "PURCHASE", "customer": "张师傅", "products": [{"name": "大米", "quantity": 30, "unit_price": 6.0}]}
            
            输入："帮李阿姨买香蕉15个单价2块"
            输出：{"action": "create_order", "order_type": "SALE", "customer": "李阿姨", "products": [{"name": "香蕉", "quantity": 15, "unit_price": 2.0}]}
            
            ===== 🔍 查询示例 =====
            输入："查询王五的订单"
            输出：{"action": "query_order", "customer": "王五"}
            
            输入："查询采购订单"
            输出：{"action": "query_order", "order_type": "PURCHASE"}
            
            输入："查询销售订单"
            输出：{"action": "query_order", "order_type": "SALE"}
            
            输入："删除订单123"
            输出：{"action": "delete_order", "order_id": 123}
            
            ===== 📊 分析示例 =====
            输入："分析这些订单"
            输出：{"action": "analyze_order"}
            
            输入："分析订单数据"
            输出：{"action": "analyze_order"}
            
            输入："帮我分析一下订单情况"
            输出：{"action": "analyze_order"}
            
            输入："订单分析"
            输出：{"action": "analyze_order"}
            
            输入："分析张三的订单"
            输出：{"action": "analyze_order", "customer": "张三"}
            
            输入："分析销售订单"
            输出：{"action": "analyze_order", "order_type": "SALE"}
            
            🔧 **提取技巧:**
            - 订单类型：优先检查采购关键词（从XX买、采购、进货），再检查销售关键词，默认销售
            - 客户/供应商：在"为/给/从/向/和/跟"后面，或"的"前面，"那里/这里/处"前面
            - 商品名：常见中文词汇（水果、食品、用品、原料、水、饮料等）
            - 数量：数字+个/件/只/袋/箱/瓶/斤等单位，或"数量X"
            - 单价：数字+元/块/钱等，或"单价/每个/一个/一瓶/价格X"
            - 多商品用逗号分隔解析
            
            🚨 **严格要求:**
            1. 只返回JSON，不要解释文字
            2. JSON格式必须标准，可直接解析
            3. 宁可字段为空也不要缺失必需字段
            4. 数字类型用数值，文本用字符串
            5. 订单类型必须是"SALE"或"PURCHASE"
            6. 客户名可以是任何中文或英文姓名
            """;
    }

    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(String analysisType) {
        String basePrompt = """
            你是专业的商业数据分析师。基于提供的数据进行深度分析。
            
            ⚠️ 重要格式要求:
            - 严禁使用 ** 星号粗体标记
            - 严禁使用任何markdown格式
            - 标题用emoji前缀，不要加星号
            - 内容直接表达，不要包围星号
            
            📊 分析要求:
            - 数据洞察要准确客观
            - 趋势判断要有依据
            - 建议要切实可行
            - 风险提示要明确
            
            💡 输出格式示例:
            🎯 关键指标总结
            • 数据项1: 具体数值
            • 数据项2: 具体数值
            
            📈 趋势分析
            • 趋势1: 简要说明
            • 趋势2: 简要说明
            
            🚀 行动建议
            • 建议1: 具体措施
            • 建议2: 具体措施
            
            请严格按照示例格式，绝不使用星号标记！
            """;
        
        return switch (analysisType.toUpperCase()) {
            case "FINANCE" -> basePrompt + "\n🏦 专注领域: 财务健康度、现金流、盈利能力分析\n请按照上述格式要求输出，不要有星号！";
            case "SALES" -> basePrompt + "\n📈 专注领域: 销售业绩、客户分析、市场趋势\n请按照上述格式要求输出，不要有星号！";
            case "INVENTORY" -> basePrompt + "\n📦 专注领域: 库存优化、周转率、供应链效率\n请按照上述格式要求输出，不要有星号！";
            case "ORDER" -> basePrompt + "\n📋 专注领域: 订单流程、客户满意度、运营效率\n请按照上述格式要求输出，不要有星号！";
            default -> basePrompt + "\n🔍 专注领域: 综合业务分析\n请按照上述格式要求输出，不要有星号！";
        };
    }

    /**
     * 构建专门的订单分析提示词 - 优化性能
     */
    private String buildOrderAnalysisPrompt() {
        return """
            你是高效的订单数据分析师。快速分析订单数据，生成简洁有用的洞察报告。
            
            ⚠️ 格式要求 - 严格遵守:
            - 绝对不要使用 ** 星号标记
            - 绝对不要使用任何markdown格式
            - 标题直接写，不要加粗体标记
            - 重要内容用emoji前缀，不要用星号包围
            
            🎯 分析重点:
            - 📊 订单概况: 总量、类型分布、状态概览
            - 💰 金额分析: 销售额、采购额、盈利情况
            - 👥 客户洞察: 主要客户、订单频率
            - 📈 趋势判断: 业务增长、模式识别
            - ⚠️ 风险提示: 异常情况、注意事项
            
            📋 输出示例格式:
            🎯 核心指标
            • 订单总数: 16个
            • 销售订单: 6个 | 采购订单: 10个
            
            💡 业务洞察  
            • 采购密集期，可能在备货
            • 客户分布良好，风险分散
            
            🚀 优化建议
            • 及时处理待确认订单
            • 关注现金流动情况
            
            ❌ 禁止格式: **标题**、**重点内容**等任何星号标记
            ✅ 正确格式: 直接写标题，用emoji区分层级
            
            请严格按照示例格式输出，不要有任何星号！
            """;
    }

    /**
     * 健康检查方法
     */
    public boolean healthCheck() {
        try {
            String testPrompt = "你好，这是一个连接测试。请简短回复'连接正常'。";
            String response = askWithCustomPrompt("测试", testPrompt);
            return response.contains("连接正常") || response.length() > 0;
        } catch (Exception e) {
            System.err.println("❌ AI服务健康检查失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取服务状态信息
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "DeepSeek AI");
        status.put("endpoint", API_URL);
        status.put("timestamp", new Date());
        
        try {
            long startTime = System.currentTimeMillis();
            boolean healthy = healthCheck();
            long responseTime = System.currentTimeMillis() - startTime;
            
            status.put("healthy", healthy);
            status.put("responseTime", responseTime + "ms");
            status.put("status", healthy ? "ACTIVE" : "INACTIVE");
        } catch (Exception e) {
            status.put("healthy", false);
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    /**
     * 兼容旧方法 - 标准请求
     */
    @Deprecated
    public String ask(String prompt) throws IOException {
        return parseCommand(prompt);
    }
} 