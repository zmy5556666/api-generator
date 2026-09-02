package org.example.apigenerator.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.apigenerator.entity.PrdAnalysisResultEntity;
import org.example.apigenerator.mapper.PrdAnalysisResultMapper;
import org.example.apigenerator.model.PrdAnalysisResult;
import org.springframework.stereotype.Service;

@Service
public class AnalysisResultService {

    private final PrdAnalysisResultMapper resultMapper;

    public AnalysisResultService(PrdAnalysisResultMapper resultMapper) {
        this.resultMapper = resultMapper;
    }

    /**
     * 将大模型的 PRD 分析结果落库保存
     */
    public void saveAnalysisResult(Long taskId, PrdAnalysisResult analysisResult) {
        PrdAnalysisResultEntity resultEntity = new PrdAnalysisResultEntity();
        resultEntity.setTaskId(taskId);
        resultEntity.setCoreEntities(analysisResult.coreEntities());
        resultEntity.setCoreActions(analysisResult.coreActions());
        resultEntity.setSummary(analysisResult.summary());

        resultMapper.insert(resultEntity);
    }

    /**
     * 根据 taskId 查询 PRD 分析结果
     */
    public PrdAnalysisResultEntity getByTaskId(Long taskId) {
        QueryWrapper<PrdAnalysisResultEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        return resultMapper.selectOne(queryWrapper);
    }

    /**
     * 根据 taskId 删除分析结果
     */
    public void deleteByTaskId(Long taskId) {
        QueryWrapper<PrdAnalysisResultEntity> query = new QueryWrapper<>();
        query.eq("task_id", taskId);
        resultMapper.delete(query);
    }
}