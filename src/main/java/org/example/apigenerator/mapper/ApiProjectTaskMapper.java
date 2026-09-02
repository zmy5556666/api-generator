package org.example.apigenerator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.apigenerator.entity.ApiProjectTask;

@Mapper
public interface ApiProjectTaskMapper extends BaseMapper<ApiProjectTask> {
    // 同样继承 BaseMapper，直接拥有 CRUD 能力
}