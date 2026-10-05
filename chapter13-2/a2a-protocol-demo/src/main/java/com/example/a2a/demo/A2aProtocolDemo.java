package com.example.a2a.demo;

import com.example.a2a.client.A2aClient;
import com.example.a2a.config.ModelRuntimeSettings;
import com.example.a2a.protocol.AgentCard;
import com.example.a2a.protocol.SendMessageResponse;
import com.example.a2a.protocol.Task;
import com.example.a2a.server.A2aHttpServer;

/**
 * A2A 协议最小端到端演示。
 */
public class A2aProtocolDemo {

    public static void main(String[] args) throws Exception {
        ModelRuntimeSettings settings = ModelRuntimeSettings.fromEnvironment();
        System.out.println("模型运行配置：" + settings.summary());
        System.out.println("本 demo 默认用确定性规则模拟 Agent，不真实请求模型。");

        try (A2aHttpServer server = new A2aHttpServer("127.0.0.1", 0)) {
            server.start();
            A2aClient client = new A2aClient(server.baseUri(), "demo-token");

            AgentCard card = client.discoverAgentCard();
            System.out.println("发现 Agent：" + card.name());
            System.out.println("Agent 技能：" + card.skills().getFirst().name());

            SendMessageResponse response = client.sendMessage("请处理工单 T-1001，用户反馈 DeepSeek 网关 504。");
            Task task = response.task();
            System.out.println("任务状态：" + task.status().state());
            System.out.println("任务 ID：" + task.id());
            System.out.println("Artifact：" + task.artifacts().getFirst().firstText());

            Task fetched = client.getTask(task.id());
            System.out.println("查询任务状态：" + fetched.status().state());
        }
    }
}
