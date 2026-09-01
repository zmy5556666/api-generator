package org.example.apigenerator.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.util.List;

@Data // 自动生成 Getter/Setter 和无参构造
@TableName(value = "prd_analysis_result", autoResultMap = true) // autoResultMap 必须开启，否则 JSON 无法解析
public class PrdAnalysisResultEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId; // 关联的主任务 ID

    // 使用 TypeHandler，MyBatis 存入时会自动把 List 转成 JSON 字符串，查出时会自动把 JSON 转回 List
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> coreEntities;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> coreActions;

    private String summary;
}