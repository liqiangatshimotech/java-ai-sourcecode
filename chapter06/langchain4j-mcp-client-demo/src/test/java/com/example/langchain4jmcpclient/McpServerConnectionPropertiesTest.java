package com.example.langchain4jmcpclient;

import com.example.langchain4jmcpclient.mcp.McpServerConnectionProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpServerConnectionPropertiesTest {

    @Test
    void buildsDemoStdioCommandForServerJar() {
        McpServerConnectionProperties properties = McpServerConnectionProperties.demoStdioCommand();

        assertThat(properties.key()).isEqualTo("support-mcp-server");
        assertThat(properties.stdioCommand()).contains("-jar");
        assertThat(properties.stdioCommand().getLast())
            .endsWith("langchain4j-mcp-server-demo-0.0.1-SNAPSHOT.jar");
    }
}
