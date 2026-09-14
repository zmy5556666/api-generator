package org.example.apigenerator.controller;

import org.example.apigenerator.dto.PrdAnalyzeRequest;
import org.example.apigenerator.entity.ApiProjectTask;
import org.example.apigenerator.entity.PrdAnalysisResultEntity;
import org.example.apigenerator.model.ApiDesignResult;
import org.example.apigenerator.service.ApiProjectService;
import org.example.apigenerator.model.ApiDesignResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;


@CrossOrigin
@RestController // 声明这是一个 Web 接口控制器，所有返回值都会自动转换成 JSON
@RequestMapping("/api/projects") // 规定这个控制器下所有接口的公共路径前缀
public class ApiProjectController {

    @Autowired
    private ApiProjectService apiProjectService;

    /**
     * 接收前端传来的 PRD 文本，调用 AI 分析并存入数据库
     */
    @PostMapping("/analyze") // 定义完整的请求路径为 POST /api/projects/analyze
    public ResponseEntity<String> analyzePrd(@RequestBody PrdAnalyzeRequest request) {

        // 调用我们已经写好并测试过的核心业务逻辑
        apiProjectService.processPrdAndSave(request.getTaskId(), request.getPrdText());

        // 返回成功提示给前端
        return ResponseEntity.ok("PRD 分析完成，数据已成功入库！");
    }

    /**
     * 创建项目接口
     * 接收前端传来的项目名称，返回生成的任务信息
     */
    @PostMapping("/create")
    public ApiProjectTask createProject(@RequestParam String projectName) {
        // 调用 Service 层创建项目，并直接将包含 taskId 的对象返回给前端
        return apiProjectService.createProject(projectName);
    }

    /**
     * 查询指定任务的 PRD 分析结果
     */
    @GetMapping("/{taskId}/analysis")
    public PrdAnalysisResultEntity getAnalysisResult(@PathVariable Long taskId) {
        // 调用 Service 层的方法并将结果直接返回给客户端
        return apiProjectService.getAnalysisResult(taskId);
    }

    /**
     * 触发二号智能体：根据任务 ID 生成 API 接口设计与 Controller 代码
     */
    @PostMapping("/{taskId}/design")
    public ApiDesignResult generateApiDesign(@PathVariable Long taskId) {
        return apiProjectService.generateApiDesign(taskId);
    }

    /**
     * 查询历史生成的 API 设计与 Controller 代码
     */
    @GetMapping("/{taskId}/design")
    public ApiDesignResult getApiDesign(@PathVariable Long taskId) {
        return apiProjectService.getApiDesign(taskId);
    }

    /**
     * 一键下载生成的完整工程代码 (ZIP 包)
     */
    @GetMapping("/{taskId}/download")
    public ResponseEntity<byte[]> downloadProjectZip(@PathVariable Long taskId) {
        try {
            // 1. 获取包含多个文件的设计结果
            ApiDesignResult design = apiProjectService.getApiDesign(taskId);
            var files = design.generatedFiles();

            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest().body("文件列表为空".getBytes(StandardCharsets.UTF_8));
            }

            // 2. 在内存中创建一个 ZIP 压缩流
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
                for (var file : files) {
                    // 处理路径：去掉可能存在的开头斜杠，防止解压路径异常
                    String filePath = file.filePath();
                    if (filePath.startsWith("/")) {
                        filePath = filePath.substring(1);
                    }

                    // 将每个文件写入 ZIP 压缩包
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(filePath);
                    zos.putNextEntry(entry);
                    zos.write(file.codeContent().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }

            // 3. 动态生成 ZIP 文件名
            String moduleName = design.moduleName() != null ? design.moduleName() : "ai-project";
            String fileName = moduleName + "-source-code.zip";

            // 4. 设置 HTTP 响应头，强制浏览器作为附件下载 ZIP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentType(MediaType.parseMediaType("application/zip"));

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("打包下载失败".getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 获取所有历史项目列表
     */
    @GetMapping("/history")
    public List<ApiProjectTask> getHistory() {
        return apiProjectService.getAllHistory();
    }

    /**
     * 删除指定历史项目
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<String> deleteTask(@PathVariable Long taskId) {
        apiProjectService.deleteTask(taskId);
        return ResponseEntity.ok("删除成功");
    }

    /**
     * 重命名项目
     */
    @PutMapping("/{taskId}/name")
    public ResponseEntity<String> renameProject(@PathVariable Long taskId, @RequestParam String newName) {
        // 调用总指挥的方法，而不是底层的 taskService
        apiProjectService.renameProject(taskId, newName);
        return ResponseEntity.ok("重命名成功");
    }
}