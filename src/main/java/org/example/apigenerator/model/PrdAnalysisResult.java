package org.example.apigenerator.model;

import java.util.List;

public record PrdAnalysisResult(
        List<String> coreEntities, // 核心业务实体，例如：["User", "Product"]
        List<String> coreActions,  // 核心操作行为，例如：["Login", "Search", "Pay"]
        String summary             // 一句话总结该需求的核心业务目标
) {}