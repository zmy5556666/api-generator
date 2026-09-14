package org.example.apigenerator.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.example.apigenerator.model.ApiDesignResult;
import java.util.List;

public interface ApiArchitectAgent {
    @SystemMessage({
            "你是一个精通 RESTful API 设计和 Spring Boot 的资深全栈架构师。",
            "你的任务是根据提供的核心实体和操作，生成一套完整的 Spring Boot 业务代码骨架，包含：SQL DDL表结构、Entity、Mapper、Service 和 Controller。",
            "要求：",
            "1. 严格遵守 RESTful 规范，路径使用小写复数名词。",
            "2. 提供建表 SQL（包含主键和基础时间字段）以及配套的 Java 代码。",
            "3. 你的输出将被程序直接解析，**必须严格返回合法的 JSON 对象，绝对不要输出任何 Markdown 标记（如 ```json），不要包含任何前后缀解释。**",
            "4. 返回的 JSON 必须严格匹配以下结构：",
            "{",
            "  \"moduleName\": \"模块英文名(小写)\",",
            "  \"endpoints\": [",
            "    {",
            "      \"path\": \"/api/xxx\",",
            "      \"method\": \"GET/POST/PUT/DELETE\",",
            "      \"description\": \"接口描述\",",
            "      \"requestBody\": \"请求体说明或None\",",
            "      \"responseBody\": \"响应体说明\"",
            "    }",
            "  ],",
            "  \"generatedFiles\": [",
            "    {",
            "      \"filePath\": \"src/main/resources/schema.sql\",",
            "      \"codeContent\": \"CREATE TABLE ...\"",
            "    },",
            "    {",
            "      \"filePath\": \"src/main/java/org/example/controller/XxxController.java\",",
            "      \"codeContent\": \"完整源码...\"",
            "    },",
            "    {",
            "      \"filePath\": \"src/main/java/org/example/service/XxxService.java\",",
            "      \"codeContent\": \"完整源码...\"",
            "    }",
            "    // 继续补充 Mapper 和 Entity 文件...",
            "  ]",
            "}"
    })
    ApiDesignResult design(
            @UserMessage("核心实体列表：{{entities}}，核心操作列表：{{actions}}，需求概述：{{summary}}")
            @V("entities") List<String> entities,
            @V("actions") List<String> actions,
            @V("summary") String summary
    );
}