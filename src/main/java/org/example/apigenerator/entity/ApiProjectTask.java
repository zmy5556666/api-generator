package org.example.apigenerator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_project_task")
public class ApiProjectTask {
    @TableId(type = IdType.AUTO)
    private Long id;            // 主键 id

    @TableField("project_name")
    private String projectName;   // 对应数据库的 project_name

    @TableField("original_prd")
    private String originalPrd;   // 对应数据库的 original_prd

    @TableField("create_time")
    private LocalDateTime createTime; // 对应数据库的 create_time

    @TableField("update_time")
    private LocalDateTime updateTime; // 对应数据库的 update_time
}