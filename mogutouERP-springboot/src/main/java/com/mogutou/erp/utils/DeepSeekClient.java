package com.mogutou.erp.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DeepSeekClient {

    private static final String API_KEY = "sk-633c1a70b16c42cbb8b02bba706ac495";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    public static String ask(String prompt) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();


        messages.add(Map.of("role", "system", "content",
                "你是一个企业管理系统助手，你的任务是将用户的自然语言指令严格转换为结构化 JSON 对象。" +
                        "返回内容必须符合如下要求：" +
                        "\n1. 只能返回 JSON 格式（以 { 开头，以 } 结尾），不要包含解释、注释、markdown标记（如 ```）。" +
                        "\n2. 只允许 action 字段取以下值：" +
                        "\n   - create_order（创建订单）" +
                        "\n   - query_sales（查询销售额）" +
                        "\n   - query_inventory（查询库存）" +
                        "\n   - delete_order（删除订单）" +
                        "\n3. 除了 action 字段外，请根据需要包含必要的参数字段，例如 supplier, product, amount, time_range 等。" +
                        "\n4. 严格输出合法 JSON（不要使用中文引号或缺少逗号），不要输出任何说明或格式包装。" +
                        "\n5. 示例输出：" +
                        "\n  { \"action\": \"create_order\", \"supplier\": \"ABC公司\", \"product\": \"螺丝刀\", \"amount\": 100 }"
        ));



        payload.put("messages", messages);

        String requestBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("❌ 请求失败：" + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            System.out.println("🤖 DeepSeek 原始返回：" + responseBody);

            // 提取 message.content
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> result = mapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = message.get("content").toString();

            // 处理 AI 返回的 markdown 格式，提取 JSON 部分
            String json = extractJsonFrom(content);
            return json;
        }
    }


    public static String askWithCustomPrompt(String prompt, String systemPrompt) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", prompt));
        payload.put("messages", messages);

        String requestBody = new ObjectMapper().writeValueAsString(payload);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("❌ 请求失败：" + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            System.out.println("🤖 DeepSeek 原始返回：" + responseBody);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> result = mapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return extractJsonFrom(message.get("content").toString());
        }
    }


    private static String extractJsonFrom(String content) {
        // 提取 ```json ... ``` 或 ``` ... ``` 中的 JSON 内容
        if (content.contains("```")) {
            int start = content.indexOf("```");
            int end = content.lastIndexOf("```");
            if (start != end) {
                return content.substring(start + 3, end).trim()
                        .replaceFirst("^json", "")  // 去掉开头的 json 标记
                        .trim();
            }
        }
        return content.trim(); // fallback：直接返回原始内容
    }


}

