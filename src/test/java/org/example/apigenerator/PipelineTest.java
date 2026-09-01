package org.example.apigenerator;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.example.apigenerator.agent.ApiArchitectAgent;
import org.example.apigenerator.agent.PrdAnalystAgent;
import org.example.apigenerator.model.ApiDesignResult;
import org.example.apigenerator.model.PrdAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

public class PipelineTest {

    @Value("${ai.agent-a.api-key}")
    private String agentAKey;
    @Value("${ai.agent-b.api-key}")
    private String agentBKey;

    @Test
    public void testFullPipeline() {
        // 1. 初始化 智能体A 的模型客户端 (贴入“需求分析师”专用的 Key)
        ChatLanguageModel analystModel = OpenAiChatModel.builder()
                .apiKey("agentAKey")
                .baseUrl("https://api.deepseek.com/v1")
                .modelName("deepseek-chat")
                .build();

        // 2. 初始化 智能体B 的模型客户端 (贴入“接口架构师”专用的 Key)
        ChatLanguageModel architectModel = OpenAiChatModel.builder()
                .apiKey("agentBKey")
                .baseUrl("https://api.deepseek.com/v1")
                .modelName("deepseek-chat")
                .build();

        // 3. 组装流水线：将各自的模型客户端绑定到对应的 Agent 接口上
        PrdAnalystAgent analyst = AiServices.builder(PrdAnalystAgent.class)
                .chatLanguageModel(analystModel) // 绑定 A 的大脑
                .build();

        ApiArchitectAgent architect = AiServices.builder(ApiArchitectAgent.class)
                .chatLanguageModel(architectModel) // 绑定 B 的大脑
                .build();

        // ====== 下面的流程代码保持不变 ======
        String prdInput = "用户可以在线选课，管理员可以发布课程，查询选课学生列表，并且支持退选操作。";

        System.out.println("====== [Phase 1] 智能体 A：需求分析中 ======");
        PrdAnalysisResult analysis = analyst.analyze(prdInput);
        System.out.println("提取实体: " + analysis.coreEntities());
        System.out.println("提取动作: " + analysis.coreActions());

        System.out.println("\n====== [Phase 2] 智能体 B：接口设计与代码生成中 ======");
        ApiDesignResult apiResult = architect.design(
                analysis.coreEntities().toString(),
                analysis.coreActions().toString(),
                analysis.summary()
        );

        System.out.println("模块名称: " + apiResult.moduleName());
        System.out.println("生成的 API 接口清单:");
        apiResult.endpoints().forEach(endpoint ->
                System.out.printf("  [%s] %-20s -> %s\n", endpoint.method(), endpoint.path(), endpoint.description())
        );
        System.out.println("\n生成的 Spring Boot 代码骨架:\n" + apiResult.generatedControllerCode());
    }
}