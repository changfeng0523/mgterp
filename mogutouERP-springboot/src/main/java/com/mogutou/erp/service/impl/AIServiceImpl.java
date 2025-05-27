package com.mogutou.erp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogutou.erp.service.AIService;
import com.mogutou.erp.service.CommandExecutorService;
import com.mogutou.erp.dto.AIRequest;
import com.mogutou.erp.dto.AIResponse;
import com.mogutou.erp.service.external.DeepSeekAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 智能AI服务实现类
 * 支持自然对话和智能指令执行
 */
@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private CommandExecutorService commandExecutor;

    @Autowired
    private DeepSeekAIService deepSeekAIService;

    private final ObjectMapper mapper = new ObjectMapper();

    // 真正需要确认的危险操作（大幅减少）
    private static final Set<String> DANGEROUS_ACTIONS = Set.of(
        "delete_order"  // 只有删除操作需要确认
    );

    // 操作描述映射
    private static final Map<String, String> ACTION_DESCRIPTIONS = Map.of(
        "create_order", "创建新订单",
        "delete_order", "删除订单",
        "confirm_order", "确认订单",
        "query_order", "查询订单信息",
        "query_sales", "查询销售数据",
        "query_inventory", "查询库存信息",
        "analyze_finance", "财务数据分析",
        "analyze_order", "订单数据分析"
    );

    @Override
    public AIResponse parseAndExecute(String input, boolean confirmed) {
        try {
            System.out.println("🎯 处理用户输入: " + input + " (已确认: " + confirmed + ")");
            
            // 第一步：智能意图识别
            IntentResult intent = analyzeIntent(input);
            
            System.out.println("🎯 意图识别结果：" + intent.type + " (置信度: " + intent.confidence + ")");
            
            switch (intent.type) {
                case COMMAND:
                    return handleCommand(input, intent.extractedCommand, confirmed);
                case CONVERSATION:
                    return handleConversation(input);
                case MIXED:
                    return handleMixedIntent(input, intent.extractedCommand, confirmed);
                default:
                    return handleConversation(input); // 默认当作对话处理
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return new AIResponse("😅 抱歉，我遇到了一些问题：" + e.getMessage(), false);
        }
    }

    @Override
    public AIResponse getBusinessInsights(AIRequest request) {
        try {
            String analysisType = request.getAnalysisType() != null ? request.getAnalysisType() : "GENERAL";
            String dataContext = request.getDataContext() != null ? request.getDataContext() : "";
            
            String insight = deepSeekAIService.analyzeData(
                request.getInput() + "\n" + dataContext, 
                analysisType
            );
            
            return new AIResponse("📊 " + insight, false);
            
        } catch (Exception e) {
            e.printStackTrace();
            return new AIResponse("😅 业务洞察分析失败：" + e.getMessage(), false);
        }
    }

    /**
     * 智能意图识别
     */
    private IntentResult analyzeIntent(String input) {
        try {
            String response = deepSeekAIService.analyzeIntent(input);
            
            System.out.println("🔍 意图分析原始回复：" + response);
            
            // 解析意图分析结果
            JsonNode result = mapper.readTree(response);
            String type = result.path("intent_type").asText("CONVERSATION");
            double confidence = result.path("confidence").asDouble(0.5);
            String extractedCommand = result.path("command").asText("");
            
            return new IntentResult(
                IntentType.valueOf(type.toUpperCase()), 
                confidence, 
                extractedCommand
            );
            
        } catch (Exception e) {
            System.out.println("⚠️ 意图识别失败，使用智能规则判断：" + e.getMessage());
            return fallbackIntentAnalysis(input);
        }
    }

    /**
     * 备用意图分析（基于关键词规则）
     */
    private IntentResult fallbackIntentAnalysis(String input) {
        String lowerInput = input.toLowerCase();
        
        // 快速识别常用指令模式
        if (lowerInput.contains("分析") && (lowerInput.contains("订单") || lowerInput.contains("这些"))) {
            System.out.println("🎯 快速识别: 订单分析指令");
            return new IntentResult(IntentType.COMMAND, 0.95, "分析订单");
        }
        
        // 指令关键词
        String[] commandKeywords = {"创建", "查询", "删除", "修改", "统计", "分析", "导出", "确认", "添加"};
        boolean hasCommandKeyword = Arrays.stream(commandKeywords)
            .anyMatch(keyword -> lowerInput.contains(keyword));
        
        // 对话关键词
        String[] conversationKeywords = {"你好", "谢谢", "再见", "怎么样", "是什么", "为什么", "天气"};
        boolean hasConversationKeyword = Arrays.stream(conversationKeywords)
            .anyMatch(keyword -> lowerInput.contains(keyword));
        
        if (hasCommandKeyword && hasConversationKeyword) {
            return new IntentResult(IntentType.MIXED, 0.8, input);
        } else if (hasCommandKeyword) {
            return new IntentResult(IntentType.COMMAND, 0.9, input);
        } else {
            return new IntentResult(IntentType.CONVERSATION, 0.7, "");
        }
    }

    /**
     * 智能处理系统指令
     */
    private AIResponse handleCommand(String input, String extractedCommand, boolean confirmed) {
        try {
            System.out.println("🎮 开始处理指令，原始输入：" + input);
            
            // 将自然语言转换为结构化指令
            String commandInput = !extractedCommand.isEmpty() ? extractedCommand : input;
            String jsonCommand = deepSeekAIService.parseCommand(commandInput);
            
            System.out.println("🎮 AI生成的JSON指令：" + jsonCommand);
            
            // 解析并验证JSON指令
            JsonNode commandNode;
            try {
                commandNode = mapper.readTree(jsonCommand);
            } catch (Exception e) {
                System.out.println("❌ JSON解析失败，尝试修复...");
                // 尝试修复常见的JSON格式问题
                String fixedJson = fixJsonFormat(jsonCommand);
                commandNode = mapper.readTree(fixedJson);
                System.out.println("✅ JSON修复成功：" + fixedJson);
            }
            
            String action = commandNode.path("action").asText();
            
            if (action.isEmpty()) {
                System.out.println("❌ 无法识别操作类型");
                return new AIResponse("😅 抱歉，我无法理解您要执行的具体操作。\n\n💡 请尝试这样说：\n" +
                    "• '为张三创建订单，商品苹果10个单价5元'\n" +
                    "• '查询本月销售额'\n" +
                    "• '删除订单123'\n" +
                    "• '查询李四的订单'", false);
            }
            
            // 增强JSON节点信息（添加原始输入便于调试）
            if (commandNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) commandNode)
                    .put("original_input", input);
            }
            
            // 危险操作确认（仅删除操作需要确认）
            if (isDangerous(action) && !confirmed) {
                String confirmMessage = generateSimpleConfirmMessage(action, commandNode, input);
                return new AIResponse(confirmMessage, true);
            }
            
            // 执行指令
            System.out.println("🚀 执行指令: " + action);
            String result = commandExecutor.execute(commandNode);
            
            // 智能结果处理
            if (result == null || result.trim().isEmpty()) {
                result = "✅ 操作已完成";
            }
            
            // 生成增强的友好回复
            String enhancedResponse = generateEnhancedResponse(result, action, commandNode, input);
            return new AIResponse(enhancedResponse, false);
            
        } catch (Exception e) {
            System.err.println("❌ 指令处理失败：" + e.getMessage());
            e.printStackTrace();
            
            // 根据错误类型提供更精准的帮助
            return generateErrorResponse(e, input);
        }
    }

    /**
     * 修复JSON格式问题
     */
    private String fixJsonFormat(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return "{}";
        }
        
        String fixed = jsonStr.trim();
        
        // 移除markdown标记
        if (fixed.startsWith("```")) {
            fixed = fixed.replaceAll("```[a-zA-Z]*", "").replaceAll("```", "").trim();
        }
        
        // 确保是有效的JSON对象
        if (!fixed.startsWith("{")) {
            fixed = "{" + fixed;
        }
        if (!fixed.endsWith("}")) {
            fixed = fixed + "}";
        }
        
        // 修复常见的JSON问题
        fixed = fixed.replace("'", "\""); // 单引号改双引号
        fixed = fixed.replaceAll("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)(\\s*:)", "$1\"$2\"$3"); // 没有引号的键名
        
        return fixed;
    }

    /**
     * 生成简洁确认消息
     */
    private String generateSimpleConfirmMessage(String action, JsonNode commandNode, String originalInput) {
        if ("delete_order".equals(action)) {
            long orderId = commandNode.path("order_id").asLong(0);
            return String.format("🗑️ 确认删除订单 %d？\n\n⚠️ 删除后无法恢复\n\n回复'是'确认，'否'取消", orderId);
        }
        
        // 其他操作的简单确认
        return String.format("⚠️ 确认执行：%s？\n\n回复'是'确认，'否'取消", getActionDescription(action));
    }

    /**
     * 生成增强的响应消息
     */
    private String generateEnhancedResponse(String result, String action, JsonNode commandNode, String originalInput) {
        // 如果执行结果已经很完善，直接返回
        if (result.contains("✅") || result.contains("❌") || result.length() > 50) {
            return result;
        }
        
        // 否则生成增强回复
        StringBuilder response = new StringBuilder();
        
        String emoji = getActionEmoji(action);
        String description = getActionDescription(action);
        
        response.append(emoji).append(" ").append(description).append("完成\n\n");
        response.append(result);
        
        // 添加相关建议
        appendRelatedSuggestions(response, action);
        
        return response.toString();
    }

    /**
     * 添加相关操作建议
     */
    private void appendRelatedSuggestions(StringBuilder response, String action) {
        response.append("\n\n💡 您还可以：\n");
        
        switch (action) {
            case "create_order":
                response.append("• 查询刚创建的订单\n• 确认订单并设置运费\n• 查看今日订单统计");
                break;
            case "query_order":
                response.append("• 查询销售数据\n• 分析订单趋势\n• 导出订单报表");
                break;
            case "query_sales":
                response.append("• 查看详细订单\n• 分析客户数据\n• 生成销售报告");
                break;
            default:
                response.append("• 继续其他操作\n• 查看系统帮助");
        }
    }

    /**
     * 生成错误响应
     */
    private AIResponse generateErrorResponse(Exception e, String input) {
        String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
        
        StringBuilder response = new StringBuilder();
        response.append("😅 处理过程中遇到问题：\n\n");
        
        // 根据错误类型提供针对性建议
        if (errorMsg.contains("JSON")) {
            response.append("🔧 **解决建议：**\n");
            response.append("• 请尝试更简单的表达\n");
            response.append("• 确保包含必要信息（如客户名、商品名）\n");
            response.append("• 例如：'为张三创建订单，苹果10个，单价5元'\n");
        } else if (errorMsg.contains("timeout") || errorMsg.contains("连接")) {
            response.append("🌐 **网络问题：**\n");
            response.append("• 请稍后重试\n");
            response.append("• 检查网络连接\n");
        } else {
            response.append("🛠️ **通用建议：**\n");
            response.append("• 重新整理表达方式\n");
            response.append("• 确保信息完整清晰\n");
            response.append("• 可以先尝试简单操作\n");
        }
        
        response.append("\n💬 您的输入：").append(input);
        response.append("\n🔧 技术细节：").append(errorMsg);
        
        return new AIResponse(response.toString(), false);
    }

    /**
     * 处理对话
     */
    private AIResponse handleConversation(String input) {
        try {
            String response = deepSeekAIService.chat(input);
            return new AIResponse(response, false);
        } catch (Exception e) {
            e.printStackTrace();
            return new AIResponse("😅 对话处理出错：" + e.getMessage(), false);
        }
    }

    /**
     * 处理混合意图
     */
    private AIResponse handleMixedIntent(String input, String extractedCommand, boolean confirmed) {
        try {
            // 先处理指令部分
            AIResponse commandResult = handleCommand(input, extractedCommand, confirmed);
            
            if (commandResult.isNeedConfirm()) {
                return commandResult; // 需要确认时直接返回
            }
            
            // 再生成对话式的友好回复
            String contextPrompt = String.format(
                "用户说：%s\n执行结果：%s\n\n请生成一个自然友好的回复，既确认操作结果，又体现对话的温暖感。回复要简洁不啰嗦。",
                input, commandResult.getReply()
            );
            
            String friendlyResponse = deepSeekAIService.askWithCustomPrompt(contextPrompt,
                "你是友好的AI助手小蘑菇。将操作结果包装成自然对话式的回复，保持轻松友好的语调。");
            
            return new AIResponse(friendlyResponse, false);
            
        } catch (Exception e) {
            e.printStackTrace();
            return new AIResponse("😅 处理请求时遇到问题：" + e.getMessage(), false);
        }
    }

    /**
     * 判断是否为危险操作
     */
    private boolean isDangerous(String action) {
        return DANGEROUS_ACTIONS.contains(action);
    }

    /**
     * 生成确认消息
     */
    private String generateConfirmMessage(String action, JsonNode commandNode) {
        String actionDesc = getActionDescription(action);
        StringBuilder confirmMsg = new StringBuilder();
        
        confirmMsg.append("🤔 检测到敏感操作：").append(actionDesc).append("\n\n");
        
        // 根据不同操作类型添加具体信息
        switch (action) {
            case "create_order":
                String customer = commandNode.path("customer").asText("未指定客户");
                confirmMsg.append("📝 将要创建订单：\n");
                confirmMsg.append("• 客户：").append(customer).append("\n");
                JsonNode products = commandNode.path("products");
                if (products.isArray() && products.size() > 0) {
                    confirmMsg.append("• 商品数量：").append(products.size()).append("种\n");
                }
                break;
                
            case "delete_order":
                String orderId = commandNode.path("order_id").asText();
                if (!orderId.isEmpty()) {
                    confirmMsg.append("🗑️ 将要删除订单ID：").append(orderId).append("\n");
                }
                break;
                
            case "confirm_order":
                String confirmOrderId = commandNode.path("order_id").asText();
                double freight = commandNode.path("freight").asDouble(0);
                confirmMsg.append("✅ 将要确认订单：\n");
                if (!confirmOrderId.isEmpty()) {
                    confirmMsg.append("• 订单ID：").append(confirmOrderId).append("\n");
                }
                if (freight > 0) {
                    confirmMsg.append("• 运费：").append(freight).append("元\n");
                }
                break;
        }
        
        confirmMsg.append("\n🚨 此操作不可撤销，确定要继续吗？");
        return confirmMsg.toString();
    }

    /**
     * 获取操作描述
     */
    private String getActionDescription(String action) {
        return ACTION_DESCRIPTIONS.getOrDefault(action, "未知操作");
    }

    /**
     * 生成友好的操作结果回复
     */
    private String generateFriendlyResponse(String result, String action, JsonNode commandNode) {
        String emoji = getActionEmoji(action);
        
        // 如果结果已经很友好了，直接返回
        if (result.contains("✅") || result.contains("❌") || result.contains("📊")) {
            return result;
        }
        
        // 否则添加emoji和友好语调
        StringBuilder response = new StringBuilder();
        response.append(emoji).append(" ");
        
        switch (action) {
            case "create_order":
                response.append("订单创建成功！\n").append(result);
                break;
            case "query_order":
                response.append("为您查询到以下订单信息：\n").append(result);
                break;
            case "delete_order":
                response.append("订单删除完成。\n").append(result);
                break;
            case "confirm_order":
                response.append("订单确认成功！\n").append(result);
                break;
            case "query_sales":
                response.append("销售数据查询结果：\n").append(result);
                break;
            case "query_inventory":
                response.append("库存信息如下：\n").append(result);
                break;
            default:
                response.append(result);
        }
        
        return response.toString();
    }

    /**
     * 获取操作对应的emoji
     */
    private String getActionEmoji(String action) {
        return switch (action) {
            case "create_order" -> "📝";
            case "query_order" -> "🔍";
            case "delete_order" -> "🗑️";
            case "confirm_order" -> "✅";
            case "query_sales" -> "💰";
            case "query_inventory" -> "📦";
            case "analyze_finance" -> "📊";
            default -> "🤖";
        };
    }

    /**
     * 意图识别结果内部类
     */
    private static class IntentResult {
        public final IntentType type;
        public final double confidence;
        public final String extractedCommand;

        public IntentResult(IntentType type, double confidence, String extractedCommand) {
            this.type = type;
            this.confidence = confidence;
            this.extractedCommand = extractedCommand;
        }
    }

    /**
     * 意图类型枚举
     */
    private enum IntentType {
        COMMAND,     // 纯指令执行
        CONVERSATION, // 纯对话交流
        MIXED        // 混合意图
    }
} 