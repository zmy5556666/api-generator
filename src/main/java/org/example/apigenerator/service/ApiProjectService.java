package org.example.apigenerator.service;

import org.example.apigenerator.agent.ApiArchitectAgent;
import org.example.apigenerator.agent.CodeReviewerAgent;
import org.example.apigenerator.agent.PrdAnalystAgent;
import org.example.apigenerator.entity.ApiProjectTask;
import org.example.apigenerator.entity.PrdAnalysisResultEntity;
import org.example.apigenerator.model.ApiDesignResult;
import org.example.apigenerator.model.PrdAnalysisResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApiProjectService {

    // 1. 注入 3 个智能体 + 3 个专职领域 Service
    private final PrdAnalystAgent prdAnalystAgent;
    private final ApiArchitectAgent apiArchitectAgent;
    private final ProjectTaskService projectTaskService;
    private final AnalysisResultService analysisResultService;
    private final ApiDesignService apiDesignService;
    private final CodeReviewerAgent codeReviewerAgent;

    public ApiProjectService(PrdAnalystAgent prdAnalystAgent,
                             ApiArchitectAgent apiArchitectAgent,
                             ProjectTaskService projectTaskService,
                             AnalysisResultService analysisResultService,
                             ApiDesignService apiDesignService,
                             CodeReviewerAgent codeReviewerAgent) {
        this.prdAnalystAgent = prdAnalystAgent;
        this.apiArchitectAgent = apiArchitectAgent;
        this.projectTaskService = projectTaskService;
        this.analysisResultService = analysisResultService;
        this.apiDesignService = apiDesignService;
        this.codeReviewerAgent = codeReviewerAgent;
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
        //收到前端发来的 PRD 后，立刻更新到主表中
        projectTaskService.updateTaskPrd(taskId, prdText);
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
     * 核心流 B: 执行 API 设计 (草稿) -> 代码审查 (定稿) -> 级联保存
     */
    @Transactional
    public ApiDesignResult generateApiDesign(Long taskId) {
        PrdAnalysisResultEntity analysisResult = analysisResultService.getByTaskId(taskId);
        if (analysisResult == null) {
            throw new IllegalArgumentException("未找到任务 ID为" + taskId + "的分析结果,请先执行PRD分析!");
        }

        // 第一步：唤醒二号智能体（架构师）生成草稿
        System.out.println("====== [Phase 2] 架构师正在生成代码草稿... ======");
        ApiDesignResult draftResult = apiArchitectAgent.design(
                analysisResult.getCoreEntities(),
                analysisResult.getCoreActions(),
                analysisResult.getSummary()
        );

        // 第二步：唤醒三号智能体（技术总监）进行 Code Review 与修复
        System.out.println("====== [Phase 2] Tech Lead 正在进行代码审查与修复... ======");
        ApiDesignResult finalResult = codeReviewerAgent.review(draftResult);

        // 第三步：交给底层 Service 级联存储最终定稿
        apiDesignService.saveDesign(taskId, finalResult);
        System.out.println("====== [Phase 2] 代码审查通过，最终版本已落库！ ======");

        return finalResult;
    }

    /**
     * 5. 辅助流：查询最终设计和代码
     */
    public ApiDesignResult getApiDesign(Long taskId) {
        return apiDesignService.getDesignByTaskId(taskId);
    }

    /**
     * 6. 辅助流：获取所有历史记录
     */
    public List<ApiProjectTask> getAllHistory() {
        return projectTaskService.getAllTasks();
    }

    /**
     * 7. 辅助流：安全级联删除指定历史记录
     */
    @Transactional
    public void deleteTask(Long taskId) {
        // 按照“先清外围，再清核心”的顺序安全删除：
        // 1. 先清理分析结果表
        analysisResultService.deleteByTaskId(taskId);

        // 2. 再清理设计报告与接口表
        apiDesignService.deleteByTaskId(taskId);

        // 3. 呼叫主服务：最后删除任务主表
        projectTaskService.deleteTask(taskId);
    }

    /**
     * 8. 辅助流：修改项目名称
     */
    public void renameProject(Long taskId, String newName) {
        projectTaskService.updateProjectName(taskId, newName);
    }

    /**
     * 9. 核心流C：保存自愈修复后的最新代码（覆盖旧数据）
     */
    @Transactional
    public void saveUpdatedDesign(Long taskId, ApiDesignResult finalDesign) {
        // 先清理掉该任务旧的生成记录和接口记录，防止数据库里出现双份数据
        apiDesignService.deleteByTaskId(taskId);

        // 把带着自愈修复代码的 finalDesign 重新级联保存进去
        apiDesignService.saveDesign(taskId, finalDesign);
    }
}