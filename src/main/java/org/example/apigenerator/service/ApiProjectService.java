package org.example.apigenerator.service;

import org.example.apigenerator.agent.PrdAnalystAgent;
import org.example.apigenerator.entity.PrdAnalysisResultEntity;
import org.example.apigenerator.mapper.PrdAnalysisResultMapper;
import org.example.apigenerator.model.PrdAnalysisResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiProjectService {

    private final PrdAnalystAgent prdAnalystAgent;
    private final PrdAnalysisResultMapper resultMapper; // MyBatis-Plus 的 Mapper

    public ApiProjectService(PrdAnalystAgent prdAnalystAgent, PrdAnalysisResultMapper resultMapper) {
        this.prdAnalystAgent = prdAnalystAgent;
        this.resultMapper = resultMapper;
    }

    @Transactional // 开启事务
    public void processPrdAndSave(Long taskId, String prdText) {
        // 1. 调用 AI 智能体，获取不可变的 Record (DTO)
        PrdAnalysisResult aiResultRecord = prdAnalystAgent.analyze(prdText);

        // 2. 创建可变的数据库实体对象 (Entity)
        PrdAnalysisResultEntity entity = new PrdAnalysisResultEntity();

        // 3. 将 Record 中的数据转移到 Entity 中 (手动映射)
        entity.setTaskId(taskId);
        entity.setCoreEntities(aiResultRecord.coreEntities()); // Record 获取属性不需要 get 前缀
        entity.setCoreActions(aiResultRecord.coreActions());
        entity.setSummary(aiResultRecord.summary());

        // 4. 保存进 MySQL 数据库
        resultMapper.insert(entity);

        System.out.println("AI 分析结果已成功存入数据库！");
    }
}
