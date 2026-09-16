package org.example.apigenerator.controller;

import org.springframework.stereotype.Component;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/build-log/{taskId}")
@Component
public class BuildLogWebSocketServer {

    // 存放每个 taskId 对应的 WebSocket Session，支持并发
    private static final ConcurrentHashMap<Long, Session> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("taskId") Long taskId) {
        sessionMap.put(taskId, session);
        System.out.println("WebSocket 建立连接，任务 ID: " + taskId);
        sendMessage(taskId, ">>> [系统提示] WebSocket 日志通道已连接，准备就绪...\n");
    }

    @OnClose
    public void onClose(@PathParam("taskId") Long taskId) {
        sessionMap.remove(taskId);
        System.out.println("WebSocket 连接断开，任务 ID: " + taskId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket 发生错误: " + error.getMessage());
    }

    /**
     * 提供给外部（比如 Docker 沙盒服务）调用的方法，用于实时发送日志
     */
    public static void sendMessage(Long taskId, String message) {
        Session session = sessionMap.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}