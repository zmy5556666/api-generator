package org.example.apigenerator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.apigenerator.entity.PrdAnalysisResultEntity;

@Mapper // 必须加这个注解，告诉 Spring 这是一个 MyBatis Mapper 接口
public interface PrdAnalysisResultMapper extends BaseMapper<PrdAnalysisResultEntity> {
    // 继承 BaseMapper 后，你已经可以直接使用 insert(), selectById(), updateById() 等方法了
    // 复杂的自定义 SQL 也可以写在这里
}
