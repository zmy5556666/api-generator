package org.example.apigenerator.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.example.apigenerator.model.ApiDesignResult;
import java.util.List;

public interface ApiArchitectAgent {

    @SystemMessage({
            "你是一个精通 RESTful API 设计和 Spring Boot 的资深接口架构师 (API Architect)。",
            "你的任务是根据需求分析师提供的【核心实体】和【核心操作】，设计标准、规范的 RESTful API 接口。",
            "要求：",
            "1. 严格遵守 RESTful 规范：路径使用小写复数名词，HTTP 方法（GET/POST/PUT/DELETE）语义准确。",
            "2. 生成一套标准的 Spring Boot @RestController 代码骨架，包含必要的注解和 DTO 类签名。",
            "严格按照返回对象的结构输出内容，不要有任何多余的解释或前后缀说明。"
    })
    ApiDesignResult design(
            @UserMessage("核心实体列表: {{entities}}, 核心操作列表: {{actions}}, 需求概述: {{summary}}")
            @V("entities") List<String>entities,
            @V("actions") List<String> actions,
            @V("summary") String summary
    );
}