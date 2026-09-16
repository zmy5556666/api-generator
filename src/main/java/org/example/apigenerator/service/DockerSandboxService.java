package org.example.apigenerator.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import org.example.apigenerator.controller.BuildLogWebSocketServer;
import org.example.apigenerator.agent.DebuggerAgent;
import org.example.apigenerator.model.ApiDesignResult;
import org.example.apigenerator.model.GeneratedFile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DockerSandboxService {

    private final DebuggerAgent debuggerAgent;
    private static final int MAX_RETRIES = 3; // 设置最大自愈次数

    // 我们使用标准的 Maven + JDK 17 镜像作为执行沙盒
    private static final String MAVEN_IMAGE = "maven:3.9.6-eclipse-temurin-17";

    public DockerSandboxService(DebuggerAgent debuggerAgent) {
        this.debuggerAgent = debuggerAgent;
    }

    /**
     * 【新入口】带自愈能力的沙箱执行方法
     */
    public ApiDesignResult executeWithSelfHealing(Long taskId, ApiDesignResult initialDesign) {
        ApiDesignResult currentDesign = initialDesign;
        int attempt = 0;
        boolean isSuccess = false;

        while (attempt <= MAX_RETRIES) {
            BuildLogWebSocketServer.sendMessage(taskId, "\n======================================================\n");
            BuildLogWebSocketServer.sendMessage(taskId, ">>> [自愈引擎] 第 " + (attempt + 1) + " 次尝试在 Docker 沙箱中编译和测试代码...\n");

            // 1. 调用底层的 Docker 物理执行逻辑
            SandboxResult result = runCodeInDocker(taskId, currentDesign.generatedFiles());

            // 2. 如果执行成功，跳出循环
            if (result.isSuccess()) {
                BuildLogWebSocketServer.sendMessage(taskId, ">>> [自愈引擎] 测试通过！代码完全健康！\n");
                isSuccess = true;
                break;
            }

            // 3. 如果执行失败，且还有重试机会，启动自愈
            if (attempt < MAX_RETRIES) {
                BuildLogWebSocketServer.sendMessage(taskId, ">>> [自愈引擎] 发现报错！正在呼叫 DebuggerAgent 分析日志并自动修复代码...\n");
                String errorLog = result.getErrorMessage();

                try {
                    // 呼叫大模型，传入当前的完整结构和报错日志
                    currentDesign = debuggerAgent.fixBuildErrors(currentDesign, errorLog);
                    BuildLogWebSocketServer.sendMessage(taskId, ">>> [自愈引擎] DebuggerAgent 已生成新的修复代码，准备开启下一轮重试...\n");
                } catch (Exception e) {
                    BuildLogWebSocketServer.sendMessage(taskId, ">>> [自愈引擎] 呼叫 DebuggerAgent 失败：" + e.getMessage() + "\n");
                    break; // 如果调用大模型报错（比如超时、网络问题），直接中断自愈
                }
            } else {
                BuildLogWebSocketServer.sendMessage(taskId, ">>> [自愈引擎] 已达到最大自愈次数，放弃治疗。\n");
            }
            attempt++;
        }

        return currentDesign; // 返回最终版本（可能是修好的，也可能是没救活的）
    }

    /**
     * 【被改造的底层逻辑】负责真实的 Docker 执行、日志收集和退出码判断
     */
    private SandboxResult runCodeInDocker(Long taskId, List<GeneratedFile> files) {
        StringBuilder logBuilder = new StringBuilder(); // 【魔法1】用于在内存中暂存所有的编译日志
        boolean isSuccess = false;

        try {
            BuildLogWebSocketServer.sendMessage(taskId, ">>> [1/4] 正在准备沙盒运行环境...\n");

            // 1. 将内存中的虚拟文件(VFS)物理写入宿主机的临时目录
            String tempDir = System.getProperty("java.io.tmpdir") + "api-generator-tasks" + File.separator + taskId;
            File workDir = new File(tempDir);
            if (workDir.exists()) {
                deleteDirectory(workDir); // 清理旧数据，保证每次运行环境纯净
            }
            for (GeneratedFile file : files) {
                File targetFile = new File(workDir, file.filePath());
                targetFile.getParentFile().mkdirs();
                Files.writeString(targetFile.toPath(), file.codeContent());
            }
            BuildLogWebSocketServer.sendMessage(taskId, ">>> [2/4] 文件持久化完毕，宿主机挂载路径：" + tempDir + "\n");

            // 2. 初始化 Docker 客户端
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .build();
            DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

            BuildLogWebSocketServer.sendMessage(taskId, ">>> [3/4] 正在检查沙盒镜像缓存...\n");
            boolean imageExists = false;
            try {
                java.util.List<com.github.dockerjava.api.model.Image> images =
                        dockerClient.listImagesCmd().withImageNameFilter(MAVEN_IMAGE).exec();
                if (images != null && !images.isEmpty()) {
                    imageExists = true;
                }
            } catch (Exception e) {
                System.out.println("查询本地镜像失败: " + e.getMessage());
            }

            if (!imageExists) {
                BuildLogWebSocketServer.sendMessage(taskId, ">>> [警告] 本地未找到镜像，正在尝试连接 Docker Hub...\n");
                dockerClient.pullImageCmd(MAVEN_IMAGE).start().awaitCompletion(10, TimeUnit.MINUTES);
            } else {
                BuildLogWebSocketServer.sendMessage(taskId, ">>> [系统提示] 已命中本地镜像缓存，跳过网络拉取！\n");
            }

            // 3. 创建容器
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(new Bind(tempDir, new Volume("/app")));

            CreateContainerResponse container = dockerClient.createContainerCmd(MAVEN_IMAGE)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/app")
                    .withCmd("mvn", "clean", "test", "-B") // 强制执行测试用例
                    .exec();

            BuildLogWebSocketServer.sendMessage(taskId, ">>> [4/4] 隔离沙盒启动成功！开始执行 Maven 测试...\n======================================================\n");

            // 启动容器
            dockerClient.startContainerCmd(container.getId()).exec();

            // 4. 实时捕获并推送日志，同时写入 StringBuilder
            dockerClient.logContainerCmd(container.getId())
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallbackTemplate<ResultCallbackTemplate<?, Frame>, Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            String logLine = new String(frame.getPayload());
                            // 一份推给前端 WebSocket
                            BuildLogWebSocketServer.sendMessage(taskId, logLine);
                            // 【魔法2】一份存入内存，供自愈时分析
                            logBuilder.append(logLine);
                        }
                    }).awaitCompletion(5, TimeUnit.MINUTES);

            // 获取容器退出码，判断成功还是失败
            Long exitCode = dockerClient.inspectContainerCmd(container.getId()).exec().getState().getExitCodeLong();
            if (exitCode != null && exitCode == 0L) {
                isSuccess = true;
            } else {
                isSuccess = false;
                logBuilder.append("\n[系统警告] 容器非正常退出，退出码: ").append(exitCode);
            }

            BuildLogWebSocketServer.sendMessage(taskId, "\n======================================================\n>>> [系统提示] 容器执行完毕，正在清理阅后即焚沙盒。\n");

            // 5. 阅后即焚
            dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();

        } catch (Exception e) {
            String errorMsg = ">>> [沙盒引擎异常] " + e.getMessage() + "\n";
            BuildLogWebSocketServer.sendMessage(taskId, errorMsg);
            logBuilder.append(errorMsg);
            e.printStackTrace();
            isSuccess = false;
        }

        // 返回包含执行结果和完整日志的对象
        return new SandboxResult(isSuccess, logBuilder.toString());
    }

    // 辅助方法：递归删除目录
    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        file.delete();
    }

    // 内部封装类：沙盒执行结果
    private static class SandboxResult {
        private boolean success;
        private String errorMessage;

        public SandboxResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}