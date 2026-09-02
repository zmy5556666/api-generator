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
     * 一键下载生成的 Controller 源码文件
     */
    @GetMapping("/{taskId}/download")
    public ResponseEntity<byte[]> downloadControllerCode(@PathVariable Long taskId) {
        // 1. 复用刚才写好的查询逻辑，获取 API 设计结果
        ApiDesignResult design = apiProjectService.getApiDesign(taskId);
        String code = design.generatedControllerCode();

        // 2. 动态生成文件名，比如 moduleName 是 "shopping"，文件名就是 "ShoppingController.java"
        String moduleName = design.moduleName() != null ? design.moduleName() : "Api";
        // StringUtils.capitalize 可以把首字母大写
        String fileName = StringUtils.capitalize(moduleName) + "Controller.java";

        // 3. 构造 HTTP 响应头，告诉浏览器这是一个需要下载的附件
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", fileName);
        // application/octet-stream 表示这是一个二进制流，强制浏览器下载
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        // 4. 将源码字符串转成字节数组，包装成 ResponseEntity 返回
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(code.getBytes(StandardCharsets.UTF_8));
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
}