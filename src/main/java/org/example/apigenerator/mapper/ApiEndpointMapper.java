package org.example.apigenerator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.apigenerator.entity.ApiEndpointEntity;

@Mapper
public interface ApiEndpointMapper extends BaseMapper<ApiEndpointEntity> {
}