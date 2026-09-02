package org.example.apigenerator.dto;

import lombok.Data;

@Data // 自动生成 getter/setter
public class PrdAnalyzeRequest {
    private Long taskId;
    private String prdText;
}