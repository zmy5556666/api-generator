package org.example.apigenerator.model;

public record ApiEndpoint(
        String path,        // 路径，例如: /api/books, /api/users/login
        String method,      // 请求方法，例如: GET, POST, PUT, DELETE
        String description, // 接口功能描述
        String requestBody, // 请求体格式简要说明（如无则填 "None"）
        String responseBody // 响应体格式简要说明
) {}