package com.mogutou.erp.nli.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogutou.erp.nli.NLIService;
import com.mogutou.erp.nli.executor.NLICommandExecutor;
import com.mogutou.erp.nli.model.NLIResponse;
import com.mogutou.erp.utils.DeepSeekClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NLIServiceImpl implements NLIService {

    @Autowired
    private NLICommandExecutor executor;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public NLIResponse parseInput(String input, boolean confirmed) {
        try {
            // 🔧 建议统一调用加 prompt 的版本（你已实现 askWithCustomPrompt）
            String raw = DeepSeekClient.askWithCustomPrompt(input,
                    "你是一个企业订单助手，负责理解用户的自然语言请求，并将其转换为结构化 JSON 指令。" +
                            "\n你需要智能判断用户想要的操作类型，并生成以下结构：" +
                            "\n\n通用结构：" +
                            "\n{" +
                            "\n  \"action\": \"create_order | query_order | delete_order | confirm_order\"," +
                            "\n  // 以下字段根据 action 类型决定是否需要" +
                            "\n  \"order_type\": \"PURCHASE | SALE\",         // 创建/查询订单使用" +
                            "\n  \"supplier\": \"京东\",                      // 创建订单使用" +
                            "\n  \"goods\": [                                 // 创建订单使用" +
                            "\n    { \"product\": \"螺丝刀\", \"quantity\": 100, \"unit_price\": 2.5 }" +
                            "\n  ]," +
                            "\n  \"order_id\": 123,                            // 删除/确认订单使用" +
                            "\n  \"freight\": 20.0,                             // 确认订单使用" +
                            "\n  \"keyword\": \"操作系统\"                     // 查询订单使用" +
                            "\n}" +
                            "\n\n📌 输出只能是 JSON 格式，不能加解释、markdown 或注释。" +
                            "\n📌 不确定字段可省略，但 action 字段必须有。");



            // 👀 调试日志
            System.out.println("🧩 AI 原始回复内容：\n" + raw);

            JsonNode root = mapper.readTree(raw);
            String action = root.path("action").asText();

            // 二次确认逻辑
            if (isSensitive(action) && !confirmed) {
                return new NLIResponse("⚠️ 检测到敏感操作（" + action + "），是否继续？", true);
            }

            // 调用指令分发器
            String result = executor.execute(root);
            return new NLIResponse(result, false);

        } catch (Exception e) {
            return new NLIResponse("❌ AI调用失败：" + e.getMessage(), false);
        }
    }

    private boolean isSensitive(String action) {
        return action.equals("delete_order") || action.equals("delete_goods") || action.equals("reset_password");
    }
}
