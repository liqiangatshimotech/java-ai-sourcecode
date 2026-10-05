package com.example.chapter09;

import com.example.chapter09.gateway.OpenClawGatewayDemo;
import com.example.chapter09.hermes.HermesAgentArchitectureDemo;
import com.example.chapter09.memory.AgentMemoryDemo;
import com.example.chapter09.sourcemap.ClaudeCodeSourceCatalogDemo;

public class Chapter09Demo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        System.out.println("Provider: " + settings.summary());
        System.out.println();
        new ClaudeCodeSourceCatalogDemo().run();
        System.out.println();
        new OpenClawGatewayDemo(settings).run();
        System.out.println();
        new AgentMemoryDemo(settings).run();
        System.out.println();
        new HermesAgentArchitectureDemo(settings).run();
    }
}
