package org.example.apigenerator.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.apigenerator.entity.ApiDesignResultEntity;
import org.example.apigenerator.entity.ApiEndpointEntity;
import org.example.apigenerator.mapper.ApiDesignResultMapper;
import org.example.apigenerator.mapper.ApiEndpointMapper;
import org.example.apigenerator.model.ApiDesignResult;
import org.example.apigenerator.model.ApiEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApiDesignService {

    private final ApiDesignResultMapper apiDesignResultMapper;
    private final ApiEndpointMapper apiEndpointMapper;

    public ApiDesignService(ApiDesignResultMapper apiDesignResultMapper, ApiEndpointMapper apiEndpointMapper) {
        this.apiDesignResultMapper = apiDesignResultMapper;
        this.apiEndpointMapper = apiEndpointMapper;
    }

    /**
     * 级联保存 API 设计总报告与单接口记录
     */
    @Transactional
    public void saveDesign(Long taskId, ApiDesignResult designResult) {
        // 1. 保存总报告
        ApiDesignResultEntity designEntity = new ApiDesignResultEntity();
        designEntity.setTaskId(taskId);
        designEntity.setModuleName(designResult.moduleName());
        designEntity.setGeneratedFiles(designResult.generatedFiles());
        designEntity.setCreateTime(LocalDateTime.now());
        apiDesignResultMapper.insert(designEntity);

        // 2. 保存接口列表
        if (designResult.endpoints() != null) {
            for (ApiEndpoint endpoint : designResult.endpoints()) {
                ApiEndpointEntity endpointEntity = new ApiEndpointEntity();
                endpointEntity.setDesignResultId(designEntity.getId());
                endpointEntity.setPath(endpoint.path());
                endpointEntity.setMethod(endpoint.method());
                endpointEntity.setDescription(endpoint.description());
                endpointEntity.setRequestBody(endpoint.requestBody());
                endpointEntity.setResponseBody(endpoint.responseBody());
                endpointEntity.setCreateTime(LocalDateTime.now());
                apiEndpointMapper.insert(endpointEntity);
            }
        }
    }

    /**
     * 根据 taskId 查询组装 API 设计历史记录
     */
    public ApiDesignResult getDesignByTaskId(Long taskId) {
        QueryWrapper<ApiDesignResultEntity> designQuery = new QueryWrapper<>();
        designQuery.eq("task_id", taskId);
        ApiDesignResultEntity designEntity = apiDesignResultMapper.selectOne(designQuery);

        if (designEntity == null) {
            throw new IllegalArgumentException("未找到任务 ID 为 " + taskId + " 的 API 设计历史记录！");
        }

        QueryWrapper<ApiEndpointEntity> endpointQuery = new QueryWrapper<>();
        endpointQuery.eq("design_result_id", designEntity.getId());
        List<ApiEndpointEntity> endpointEntities = apiEndpointMapper.selectList(endpointQuery);

        List<ApiEndpoint> endpoints = endpointEntities.stream().map(ent -> new ApiEndpoint(
                ent.getPath(),
                ent.getMethod(),
                ent.getDescription(),
                ent.getRequestBody(),
                ent.getResponseBody()
        )).toList();

        return new ApiDesignResult(
                designEntity.getModuleName(),
                endpoints,
                designEntity.getGeneratedFiles()
        );
    }

    /**
     * 级联删除 API 设计及其关联的单接口记录
     */
    @Transactional
    public void deleteByTaskId(Long taskId) {
        // 1. 先查出这个任务对应的设计报告（为了拿到主键 ID 去删孙子表）
        QueryWrapper<ApiDesignResultEntity> designQuery = new QueryWrapper<>();
        designQuery.eq("task_id", taskId);
        ApiDesignResultEntity designEntity = apiDesignResultMapper.selectOne(designQuery);

        if (designEntity != null) {
            // 2. 先删孙子表：删除关联的接口记录
            QueryWrapper<ApiEndpointEntity> endpointQuery = new QueryWrapper<>();
            endpointQuery.eq("design_result_id", designEntity.getId());
            apiEndpointMapper.delete(endpointQuery);

            // 3. 再删子表：删除设计总报告
            apiDesignResultMapper.deleteById(designEntity.getId());
        }
    }
}