package org.example.apigenerator.model;

import java.util.List;

public record ApiDesignResult(
        String moduleName,            // 所属模块名称
        List<ApiEndpoint> endpoints,  // 设计出的接口列表
        List<GeneratedFile> generatedFiles
) {}