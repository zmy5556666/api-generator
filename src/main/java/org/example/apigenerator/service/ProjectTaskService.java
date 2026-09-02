package org.example.apigenerator.service;

import org.example.apigenerator.entity.ApiProjectTask;
import org.example.apigenerator.mapper.ApiProjectTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@Service
public class ProjectTaskService {

    private final ApiProjectTaskMapper taskMapper;

    public ProjectTaskService(ApiProjectTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    /**
     * 创建項目
     */
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

    /**
     * 查询所有历史任务（按时间倒序排，最新的在最上面）
     */
    public List<ApiProjectTask> getAllTasks() {
        QueryWrapper<ApiProjectTask> query = new QueryWrapper<>();
        query.orderByDesc("create_time");
        return taskMapper.selectList(query);
    }

    /**
     * 根据 ID 删除任务
     */
    public void deleteTask(Long taskId) {
        taskMapper.deleteById(taskId);
    }

    public ApiProjectTask getById(Long taskId) {
        return taskMapper.selectById(taskId);
    }

    /**
     * 更新任务的原始 PRD 文本
     */
    public void updateTaskPrd(Long taskId, String prdText) {
        ApiProjectTask task = taskMapper.selectById(taskId);
        if (task != null) {
            task.setOriginalPrd(prdText);
            taskMapper.updateById(task);
        }
    }

    /**
     * 同步更新名字
     */
    public void updateProjectName(Long taskId, String newName) {
        ApiProjectTask task = new ApiProjectTask();
        task.setId(taskId);
        task.setProjectName(newName);
        taskMapper.updateById(task);
    }
}