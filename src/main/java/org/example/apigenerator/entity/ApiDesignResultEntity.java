package org.example.apigenerator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import org.example.apigenerator.model.GeneratedFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "api_design_result", autoResultMap = true)
public class ApiDesignResultEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId; // 关联 api_project_task 表的主键

    @TableField("module_name")
    private String moduleName; // 模块名称

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<GeneratedFile> generatedFiles;

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间
}