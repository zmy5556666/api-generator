/**
 * *   **Phase 2 (Day 4-7)**：设计 MySQL 数据库表结构，实现用户项目管理的增删改
 * *    查（CRUD）逻辑，打通整个后端的数据流。
 */
package org.example.apigenerator;

import org.example.apigenerator.ApiGeneratorApplication;
import org.example.apigenerator.agent.PrdAnalystAgent;
import org.example.apigenerator.entity.PrdAnalysisResultEntity; // 导入你的实体类
import org.example.apigenerator.mapper.PrdAnalysisResultMapper; // 导入你的 Mapper
import org.example.apigenerator.model.PrdAnalysisResult;
import org.example.apigenerator.service.ApiProjectService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper; // MyBatis-Plus 条件构造器
import java.util.List;

@SpringBootTest(classes = ApiGeneratorApplication.class)
@Transactional
class ApiProjectServiceTest {

    @Autowired
    private ApiProjectService apiProjectService;

    // 注入 Mapper，用于测试后的数据抽查
    @Autowired
    private PrdAnalysisResultMapper resultMapper;

    @MockBean
    private PrdAnalystAgent prdAnalystAgent;

    @Test
    void testProcessPrdAndSave() {
        // 1. 准备参数
        String prdText = "我们需要开发一个图书管理模块...";
        Long mockTaskId = 1L;

        // 2. 配置 Mock
        PrdAnalysisResult mockResult = new PrdAnalysisResult(
                List.of("User", "Order"),
                List.of("Create", "Cancel"),
                "这是一个用于测试的核心业务目标总结"
        );
        Mockito.when(prdAnalystAgent.analyze(Mockito.anyString())).thenReturn(mockResult);

        // 3. 执行核心业务
        apiProjectService.processPrdAndSave(mockTaskId, prdText);

        // 4. 断言验证 (Assertion) —— 机器自动对账的核心
        // 使用 MyBatis-Plus 从数据库中查出 task_id = 1 的数据
        List<PrdAnalysisResultEntity> savedRecords = resultMapper.selectList(
                new QueryWrapper<PrdAnalysisResultEntity>().eq("task_id", mockTaskId)
        );

        // 第一道断言：证明确实存进去了一条数据
        Assertions.assertEquals(1, savedRecords.size(), "数据库中必须只查到 1 条该任务的记录");

        // 第二道断言：证明存进去的字段没有发生错乱丢失
        PrdAnalysisResultEntity savedEntity = savedRecords.get(0);
        Assertions.assertEquals("这是一个用于测试的核心业务目标总结", savedEntity.getSummary(), "Summary 字段存取不一致！");
        // 因为你的 Entity 里存储 List 可能是用了 JSON 字符串转换，我们甚至可以断言它转换对了没有
        Assertions.assertTrue(savedEntity.getCoreEntities().contains("Order"), "核心实体未能正确保存");
    }
}