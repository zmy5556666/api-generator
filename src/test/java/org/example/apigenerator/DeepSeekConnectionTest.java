package org.example.apigenerator;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

public class DeepSeekConnectionTest {

    @Value("${ai.agent-b.api-key}")
    private String agentBKey;

    @Test
    public void testDeepSeekCall() {
        // 1. 初始化模型客户端 (利用 DeepSeek 兼容 OpenAI 的特性)
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey("agentBKey") // 替换为你创建的“需求分析师”或“接口架构师”的 Key
                .baseUrl("https://api.deepseek.com/v1") // DeepSeek 的 API 地址，注意后面要加 /v1
                .modelName("deepseek-chat") // 使用 DeepSeek 的通用对话模型
                .build();

        // 2. 发生测试对话
        System.out.println("正在呼叫 DeepSeek，请稍候...");
        String response = model.generate("你好，请扮演一名资深软件架构师，用一句话跟我打个招呼。");

        // 3. 打印结果
        System.out.println("AI 回复: " + response);
    }
}