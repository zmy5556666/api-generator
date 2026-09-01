package org.example.apigenerator;

import org.example.apigenerator.service.ApiProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApiGeneratorApplication.class) // 这个注解会启动整个 Spring Boot 环境，包括数据库连接和 Bean 注入
class ApiProjectServiceTest {

    // 自动注入我们要测试的 Service
    @Autowired
    private ApiProjectService apiProjectService;

    @Test
    void testProcessPrdAndSave() {
        // 1. 准备一段模拟的、非结构化的自然语言 PRD
        String prdText = "我们需要开发一个图书管理模块，核心业务是图书和借阅记录。用户可以执行添加图书、删除图书操作，另外还需要一个查询借阅历史的功能。";

        // 2. 设定模拟的主任务 ID
        Long mockTaskId = 1L;

        // 3. 执行核心业务逻辑
        System.out.println("========== 开始测试：调用大模型分析 PRD ==========");

        apiProjectService.processPrdAndSave(mockTaskId, prdText);

        System.out.println("========== 测试结束：请前往数据库查看数据 ==========");
    }
}