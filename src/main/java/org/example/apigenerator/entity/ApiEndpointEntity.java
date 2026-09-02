package org.example.apigenerator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_endpoint")
public class ApiEndpointEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("design_result_id")
    private Long designResultId; // 关联 api_design_result 表的主键

    @TableField("path")
    private String path; // 接口路径

    @TableField("method")
    private String method; // HTTP 方法

    @TableField("description")
    private String description; // 接口描述

    @TableField("request_body")
    private String requestBody; // 请求体结构

    @TableField("response_body")
    private String responseBody; // 响应体结构

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间
}