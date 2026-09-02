package org.example.apigenerator.service;

import org.example.apigenerator.entity.ApiProjectTask;
import org.example.apigenerator.mapper.ApiProjectTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ProjectTaskService {

    private final ApiProjectTaskMapper taskMapper;

    public ProjectTaskService(ApiProjectTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Transactional
    public ApiProjectTask createProject(String projectName) {
        ApiProjectTask task = new ApiProjectTask();
        task.setProjectName(projectName);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        // 修正非空默认值
        task.setOriginalPrd("");

        taskMapper.insert(task);
        return task;
    }

    public ApiProjectTask getById(Long taskId) {
        return taskMapper.selectById(taskId);
    }
}