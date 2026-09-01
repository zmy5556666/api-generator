package org.example.apigenerator;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.example.apigenerator.agent.PrdAnalystAgent;
import org.example.apigenerator.model.PrdAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

public class PrdAnalystTest {

    @Value("${ai.agent-a.api-key}")
    private String agentAKey;

    @Test
    public void testAgent() {
        // 1. 准备好我们刚刚跑通的模型客户端
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey("agentAKey") // 记得替换
                .baseUrl("https://api.deepseek.com/v1")
                .modelName("deepseek-chat")
                .build();

        // 2. 见证奇迹的时刻：通过 AiServices 把模型和咱们写的接口绑定起来！
        PrdAnalystAgent agent = AiServices.builder(PrdAnalystAgent.class)
                .chatLanguageModel(model)
                .build();

        // 3. 模拟一段粗糙的自然语言需求 (PRD)
        String userPrd = "我需要一个图书管理系统，管理员可以登录系统，还能查询图书列表，并且能增加新的图书。";

        System.out.println("智能体 A (需求分析师) 正在解析需求，请稍候...");

        // 4. 调用接口！注意：这里返回的直接就是你定义的 Java 对象，不再是死板的字符串！
        PrdAnalysisResult result = agent.analyze(userPrd);

        // 5. 打印验证
        System.out.println("核心实体 (Entities): " + result.coreEntities());
        System.out.println("核心操作 (Actions): " + result.coreActions());
        System.out.println("一句话总结: " + result.summary());
    }
}