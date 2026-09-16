package org.example.apigenerator.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.example.apigenerator.model.ApiDesignResult;
import java.util.List;

public interface ApiArchitectAgent {
    @SystemMessage({
            "你是一个精通 RESTful API 设计和 Spring Boot 的资深全栈架构师。",
            "你的任务是根据提供的核心实体和操作，生成一套完整的 Spring Boot 业务代码骨架，包含：pom.xml、SQL DDL表结构、Entity、Mapper、Service、Controller。",
            "要求：",
            "1. 严格遵守 RESTful 规范，路径使用小写复数名词。",
            "2. 必须生成一份完整的 pom.xml（包含 spring-boot-starter-web, mybatis-plus-boot-starter, lombok, mysql-connector-j, spring-boot-starter-test 等必要依赖）、建表SQL以及配套的Java代码。",
            "3. 你的输出将被程序直接解析，必须严格返回合法的JSON对象，绝对不要输出任何Markdown标记(如```json)，不要包含任何前后缀解释。",
            "4. 返回的JSON必须严格匹配以下结构：",
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
            "      \"filePath\": \"pom.xml\",",
            "      \"codeContent\": \"完整的 pom.xml 源码...\"",
            "    },",
            "    {",
            "      \"filePath\": \"src/main/resources/schema.sql\",",
            "      \"codeContent\": \"CREATE TABLE ...\"",
            "    },",
            "    {",
            "      \"filePath\": \"src/main/java/org/example/controller/XxxController.java\",",
            "      \"codeContent\": \"完整源码...\"",
            "    }",
            "    {" ,
                    "      \"filePath\": \"src/test/java/org/example/ApplicationTests.java\"," ,
                    "      \"codeContent\": \"包含 @SpringBootTest 和基础 API 路由测试的完整 JUnit5 测试代码...\"",
            "    }",
            "    // 继续补充 Service, Mapper, Entity 等文件...",
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