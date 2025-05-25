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
            String raw = DeepSeekClient.askWithCustomPrompt(input,
                    "你是一个企业订单管理助手，请将用户输入的自然语言转换成结构化 JSON。" +
                            "只能输出 JSON 格式，无多余文字、无 markdown。" +
                            "\\n允许的操作（action）字段如下：" +
                            "\\n  - create_order: 创建订单，需包含以下字段：" +
                            "\\n      - supplier: 供应商名称（字符串）" +
                            "\\n      - product: 商品名称（字符串）" +
                            "\\n      - amount: 数量（整数）" +
                            "\\n  - delete_order: 删除订单，需包含字段 order_id（整数）" +
                            "\\n输出格式示例：" +
                            "\\n  { \"action\": \"create_order\", \"supplier\": \"京东\", \"product\": \"螺丝刀\", \"amount\": 100 }"
            );


            JsonNode root = mapper.readTree(raw);
            String action = root.path("action").asText();

            if (isSensitive(action) && !confirmed) {
                return new NLIResponse("⚠️ 检测到敏感操作（" + action + "），是否继续？", true);
            }

            String result = executor.execute(root);
            return new NLIResponse(result, false);

        } catch (Exception e) {
            return new NLIResponse("❌ AI调用失败：" + e.getMessage(), false);
        }
    }

    private boolean isSensitive(String action) {
        return action.equals("delete_order");
    }
}

