package org.example.apigenerator.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.example.apigenerator.model.PrdAnalysisResult;

public interface PrdAnalystAgent {

    // @SystemMessage 就是给大模型设定的“系统人设”和“全局指令”
    @SystemMessage({
            "你是一个拥有十年经验的资深软件需求分析师 (PRD Analyst)。",
            "你的任务是仔细阅读用户输入的非结构化自然语言需求，从中精准提取信息。",
            "1. 提取出核心的'业务实体'（如用户、订单、商品等，请用英文大驼峰命名法）。",
            "2. 提取出核心的'操作行为'（如注册、查询、支付等，请用英文动词）。",
            "3. 用一句话概括这段需求的业务目标。",
            "严格按照返回对象的结构输出内容，不要有任何多余的解释和废话。"
    })
    PrdAnalysisResult analyze(@UserMessage String prdText);
}