package org.example.apigenerator.service;

import org.example.apigenerator.agent.ApiArchitectAgent;
import org.example.apigenerator.agent.PrdAnalystAgent;
import org.example.apigenerator.entity.ApiProjectTask;
import org.example.apigenerator.entity.PrdAnalysisResultEntity;
import org.example.apigenerator.model.ApiDesignResult;
import org.example.apigenerator.model.PrdAnalysisResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiProjectService {

    // 1. 彻底抛弃 Mapper，转而注入 2 个智能体 + 3 个专职领域 Service
    private final PrdAnalystAgent prdAnalystAgent;
    private final ApiArchitectAgent apiArchitectAgent;
    private final ProjectTaskService projectTaskService;
    private final AnalysisResultService analysisResultService;
    private final ApiDesignService apiDesignService;

    public ApiProjectService(PrdAnalystAgent prdAnalystAgent,
                             ApiArchitectAgent apiArchitectAgent,
                             ProjectTaskService projectTaskService,
                             AnalysisResultService analysisResultService,
                             ApiDesignService apiDesignService) {
        this.prdAnalystAgent = prdAnalystAgent;
        this.apiArchitectAgent = apiArchitectAgent;
        this.projectTaskService = projectTaskService;
        this.analysisResultService = analysisResultService;
        this.apiDesignService = apiDesignService;
    }

    /**
     * 1. 龙头：创建项目任务
     */
    public ApiProjectTask createProject(String projectName) {
        // 直接委托给底层 Service
        return projectTaskService.createProject(projectName);
    }

    /**
     * 2. 核心流 A：执行 PRD 分析并保存
     */
    @Transactional
    public void processPrdAndSave(Long taskId, String prdText) {
        // 前置校验
        ApiProjectTask task = projectTaskService.getById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("无效的 taskId: " + taskId + "，请先创建对应的项目任务！");
        }

        // 唤醒一号智能体
        PrdAnalysisResult analysisResult = prdAnalystAgent.analyze(prdText);

        // 交给底层 Service 存储
        analysisResultService.saveAnalysisResult(taskId, analysisResult);
        System.out.println("AI 分析结果已成功存入数据库！");
    }

    /**
     * 3. 辅助流：查询分析结果
     */
    public PrdAnalysisResultEntity getAnalysisResult(Long taskId) {
        return analysisResultService.getByTaskId(taskId);
    }

    /**
     * 4. 核心流 B：执行 API 设计并级联保存
     */
    @Transactional
    public ApiDesignResult generateApiDesign(Long taskId) {
        // 获取前置分析结果
        PrdAnalysisResultEntity analysisResult = analysisResultService.getByTaskId(taskId);
        if (analysisResult == null) {
            throw new IllegalArgumentException("未找到任务 ID 为 " + taskId + " 的分析结果，请先执行 PRD 分析！");
        }

        // 唤醒二号智能体
        ApiDesignResult designResult = apiArchitectAgent.design(
                analysisResult.getCoreEntities(),
                analysisResult.getCoreActions(),
                analysisResult.getSummary()
        );

        // 交给底层 Service 级联存储
        apiDesignService.saveDesign(taskId, designResult);

        return designResult;
    }

    /**
     * 5. 辅助流：查询最终设计和代码
     */
    public ApiDesignResult getApiDesign(Long taskId) {
        return apiDesignService.getDesignByTaskId(taskId);
    }
}