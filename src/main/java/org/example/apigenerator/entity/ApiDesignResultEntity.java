package org.example.apigenerator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_design_result")
public class ApiDesignResultEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId; // 关联 api_project_task 表的主键

    @TableField("module_name")
    private String moduleName; // 模块名称

    @TableField("generated_controller_code")
    private String generatedControllerCode; // 生成的 Spring Boot 源码

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间
}