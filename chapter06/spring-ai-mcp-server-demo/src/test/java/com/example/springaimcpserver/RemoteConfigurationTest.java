package com.example.springaimcpserver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import static org.junit.jupiter.api.Assertions.*;
class RemoteConfigurationTest {
    @Test void remoteMissingIdpSettingsCannotStart() {
        Exception failure=assertThrows(Exception.class,()->{
            try(var app=new SpringApplicationBuilder(SpringAiMcpServerApplication.class).profiles("remote")
                    .run("--server.port=0","--mcp.auth.issuer=","--mcp.auth.jwk-set-uri=","--mcp.auth.resource=")) {
                fail("缺少远程授权配置不应启动成功");
            }
        });
        Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();
        assertTrue(cause.getMessage().contains("MCP_ISSUER"),cause.toString());
    }
}
