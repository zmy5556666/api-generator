package org.example.apigenerator.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.example.apigenerator.agent.PrdAnalystAgent;
import org.example.apigenerator.agent.ApiArchitectAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 标识这是一个 Spring 配置类
public class AiAgentConfig {

    @Value("${ai.agent-a.api-key}")
    private String agentAKey;

    @Value("${ai.agent-b.api-key}")
    private String agentBKey;

    @Bean // 将返回值注册为 Spring 容器中的 Bean
    public PrdAnalystAgent prdAnalystAgent() {
        // 1. 构建智能体 A 专属的模型客户端 (使用独立的 API Key)
        OpenAiChatModel modelA = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1") // DeepSeek 的 OpenAI 兼容端点
                .apiKey(agentAKey)
                .modelName("deepseek-chat")
                .build();

        // 2. 使用 AiServices 动态代理生成接口的实现类
        return AiServices.builder(PrdAnalystAgent.class)
                .chatLanguageModel(modelA)
                // 可选：添加上下文记忆功能
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    @Bean
    public ApiArchitectAgent apiArchitectAgent() {
        // 1. 构建智能体 B 专属的模型客户端
        OpenAiChatModel modelB = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(agentBKey)
                .modelName("deepseek-chat")
                .build();

        // 2. 注册为 Bean
        return AiServices.builder(ApiArchitectAgent.class)
                .chatLanguageModel(modelB)
                .build();
    }
}
