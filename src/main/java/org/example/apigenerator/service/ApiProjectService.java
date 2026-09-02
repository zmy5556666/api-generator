package org.example.apigenerator.service;

import org.example.apigenerator.agent.ApiArchitectAgent;
import org.example.apigenerator.agent.PrdAnalystAgent;
import org.example.apigenerator.entity.ApiDesignResultEntity;
import org.example.apigenerator.entity.ApiEndpointEntity;
import org.example.apigenerator.entity.ApiProjectTask;
import org.example.apigenerator.entity.PrdAnalysisResultEntity;
import org.example.apigenerator.mapper.ApiDesignResultMapper;
import org.example.apigenerator.mapper.ApiEndpointMapper;
import org.example.apigenerator.mapper.ApiProjectTaskMapper;
import org.example.apigenerator.mapper.PrdAnalysisResultMapper;
import org.example.apigenerator.model.ApiDesignResult;
import org.example.apigenerator.model.ApiEndpoint;
import org.example.apigenerator.model.PrdAnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Service
public class ApiProjectService {
    // 1. 注入 Mapper 实例
    private final ApiArchitectAgent apiArchitectAgent;
    private final PrdAnalystAgent prdAnalystAgent;
    private final PrdAnalysisResultMapper resultMapper;
    private final ApiProjectTaskMapper apiProjectTaskMapper;
    private final ApiDesignResultMapper apiDesignResultMapper;
    private final ApiEndpointMapper apiEndpointMapper;


    public ApiProjectService(PrdAnalystAgent prdAnalystAgent,
                             PrdAnalysisResultMapper resultMapper,
                             ApiProjectTaskMapper apiProjectTaskMapper,
                             ApiArchitectAgent apiArchitectAgent,
                             ApiDesignResultMapper apiDesignResultMapper,
                             ApiEndpointMapper apiEndpointMapper) {
        this.prdAnalystAgent = prdAnalystAgent;
        this.resultMapper = resultMapper;
        this.apiProjectTaskMapper = apiProjectTaskMapper;
        this.apiArchitectAgent = apiArchitectAgent;
        this.apiDesignResultMapper = apiDesignResultMapper;
        this.apiEndpointMapper = apiEndpointMapper;
    }

    @Transactional // 开启事务
    public void processPrdAndSave(Long taskId, String prdText) {
        // 2. 使用注入的实例对象调用 selectById
        ApiProjectTask task = apiProjectTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("无效的 taskId: " + taskId + "，请先创建对应的项目任务！");
        }

        // 3. 调用大模型分析 PRD
        PrdAnalysisResult analysisResult = prdAnalystAgent.analyze(prdText);

        // 4. 安全入库
        PrdAnalysisResultEntity resultEntity = new PrdAnalysisResultEntity();
        resultEntity.setTaskId(taskId);
        resultEntity.setCoreEntities(analysisResult.coreEntities());
        resultEntity.setCoreActions(analysisResult.coreActions());
        resultEntity.setSummary(analysisResult.summary());

        resultMapper.insert(resultEntity);

        System.out.println("AI 分析结果已成功存入数据库！");
    }

    /**
     * 创建一个新的 API 项目任务
     */
    @Transactional
    public ApiProjectTask createProject(String projectName) {
        // 1. 实例化主表实体
        ApiProjectTask task = new ApiProjectTask();
        task.setProjectName(projectName);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        // 2. 插入数据库
        apiProjectTaskMapper.insert(task);

        // 3. 返回创建好的任务对象（此时 MyBatis-Plus 已经自动填入了生成的 id）
        return task;
    }

    /**
     * 根据 taskId 查询 PRD 分析结果
     */
    public PrdAnalysisResultEntity getAnalysisResult(Long taskId) {
        // 构建查询条件：查询 task_id = 传入的taskId 的记录
        QueryWrapper<PrdAnalysisResultEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);

        // 调用我们早就注入好的 resultMapper 进行单条查询并返回
        return resultMapper.selectOne(queryWrapper);
    }

    /**
     * 根据已有的 PRD 分析结果，调用二号智能体生成 API 架构代码，并持久化到数据库
     */
    @Transactional // 开启事务，保证主表和子表数据同时成功或失败
    public ApiDesignResult generateApiDesign(Long taskId) {
        // 1. 查询 PRD 分析结果
        PrdAnalysisResultEntity analysisResult = getAnalysisResult(taskId);
        if (analysisResult == null) {
            throw new IllegalArgumentException("未找到任务 ID 为 " + taskId + " 的分析结果，请先执行 PRD 分析！");
        }

        // 2. 唤醒二号智能体！获取返回结果 (DTO)
        ApiDesignResult designResult = apiArchitectAgent.design(
                analysisResult.getCoreEntities(),
                analysisResult.getCoreActions(),
                analysisResult.getSummary()
        );

        // 3. 落库处理：保存总报告到 api_design_result 表
        ApiDesignResultEntity designEntity = new ApiDesignResultEntity();
        designEntity.setTaskId(taskId);
        designEntity.setModuleName(designResult.moduleName());
        designEntity.setGeneratedControllerCode(designResult.generatedControllerCode());
        designEntity.setCreateTime(LocalDateTime.now());

        apiDesignResultMapper.insert(designEntity); // 插入后，MyBatis-Plus 会自动把生成的 ID 填回 designEntity.getId()

        // 4. 落库处理：遍历保存每一个接口到 api_endpoint 表
        if (designResult.endpoints() != null) {
            for (ApiEndpoint endpoint : designResult.endpoints()) {
                ApiEndpointEntity endpointEntity = new ApiEndpointEntity();
                endpointEntity.setDesignResultId(designEntity.getId()); // 关键：绑定刚刚生成的主表 ID
                endpointEntity.setPath(endpoint.path());
                endpointEntity.setMethod(endpoint.method());
                endpointEntity.setDescription(endpoint.description());
                endpointEntity.setRequestBody(endpoint.requestBody());
                endpointEntity.setResponseBody(endpoint.responseBody());
                endpointEntity.setCreateTime(LocalDateTime.now());

                apiEndpointMapper.insert(endpointEntity);
            }
        }

        // 5. 将大模型原始响应返回给前端展示
        return designResult;
    }

    /**
     * 根据 taskId 查询已保存的 API 设计和生成的代码
     */
    public ApiDesignResult getApiDesign(Long taskId) {
        // 1. 查询设计总报告主表 (api_design_result)
        QueryWrapper<ApiDesignResultEntity> designQuery = new QueryWrapper<>();
        designQuery.eq("task_id", taskId);
        ApiDesignResultEntity designEntity = apiDesignResultMapper.selectOne(designQuery);

        if (designEntity == null) {
            throw new IllegalArgumentException("未找到任务 ID 为 " + taskId + " 的 API 设计历史记录！");
        }

        // 2. 查询该设计下关联的所有单个接口 (api_endpoint)
        QueryWrapper<ApiEndpointEntity> endpointQuery = new QueryWrapper<>();
        endpointQuery.eq("design_result_id", designEntity.getId());
        List<ApiEndpointEntity> endpointEntities = apiEndpointMapper.selectList(endpointQuery);

        // 3. 将数据库实体类 (Entity) 重新映射为你定义的 DTO 记录类 (Record)
        List<ApiEndpoint> endpoints = endpointEntities.stream().map(ent -> new ApiEndpoint(
                ent.getPath(),
                ent.getMethod(),
                ent.getDescription(),
                ent.getRequestBody(),
                ent.getResponseBody()
        )).toList();

        // 4. 组装并返回最终结果
        return new ApiDesignResult(
                designEntity.getModuleName(),
                endpoints,
                designEntity.getGeneratedControllerCode()
        );
    }
}
