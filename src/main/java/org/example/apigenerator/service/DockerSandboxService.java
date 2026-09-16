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
import org.example.apigenerator.model.GeneratedFile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DockerSandboxService {

    // 我们使用标准的 Maven + JDK 17 镜像作为执行沙盒
    private static final String MAVEN_IMAGE = "maven:3.9.6-eclipse-temurin-17";

    public void executeCode(Long taskId, List<GeneratedFile> files) {
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

            // 2. 初始化 Docker 客户端 (会自动探测 Windows/Mac 上的 Docker Desktop)
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .build();
            DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

            // 先检查本地是否存在该镜像，避免强制联网触发 DNS 污染报错
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
                // 只有本地没有时，才尝试去云端拉取
                dockerClient.pullImageCmd(MAVEN_IMAGE).start().awaitCompletion(10, TimeUnit.MINUTES);
            } else {
                BuildLogWebSocketServer.sendMessage(taskId, ">>> [系统提示] 已命中本地镜像缓存，跳过网络拉取，直接启动沙盒！\n");
            }

            // 3. 创建容器：把我们刚才存代码的临时目录，映射到容器内部的 /app 目录
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(new Bind(tempDir, new Volume("/app")));

            CreateContainerResponse container = dockerClient.createContainerCmd(MAVEN_IMAGE)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/app")
                    // 我们先执行 compile 试水，验证代码是否能成功编译
                    .withCmd("mvn", "clean", "compile")
                    .exec();

            BuildLogWebSocketServer.sendMessage(taskId, ">>> [4/4] 隔离沙盒启动成功！开始执行 Maven 编译...\n======================================================\n");

            // 启动容器
            dockerClient.startContainerCmd(container.getId()).exec();

            // 4. 实时捕获并推送日志 (这是最魔法的一步)
            dockerClient.logContainerCmd(container.getId())
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallbackTemplate<ResultCallbackTemplate<?, Frame>, Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            // 将 Docker 容器里吐出的每一行日志，原封不动地推送到 WebSocket
                            BuildLogWebSocketServer.sendMessage(taskId, new String(frame.getPayload()));
                        }
                    }).awaitCompletion(5, TimeUnit.MINUTES); // 最多等 5 分钟

            BuildLogWebSocketServer.sendMessage(taskId, "\n======================================================\n>>> [系统提示] 容器执行完毕，正在清理阅后即焚沙盒。\n");

            // 5. 阅后即焚：执行完后强制删除容器，不留垃圾
            dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();

        } catch (Exception e) {
            BuildLogWebSocketServer.sendMessage(taskId, ">>> [沙盒引擎异常] " + e.getMessage() + "\n");
            e.printStackTrace();
        }
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
}