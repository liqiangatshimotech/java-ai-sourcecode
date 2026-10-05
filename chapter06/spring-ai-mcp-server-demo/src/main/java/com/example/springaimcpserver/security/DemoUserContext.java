package com.example.springaimcpserver.security;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import io.modelcontextprotocol.common.McpTransportContext;
import java.util.Set;
/** 仅local模式允许固定演示身份，remote必须从MCP显式上下文读取。 */
@Component
public class DemoUserContext {
    private final boolean remote;
    public DemoUserContext(Environment env){remote=env.acceptsProfiles(Profiles.of("remote"));}
    public CurrentUser currentUser(McpTransportContext context) {
        if(!remote) return new CurrentUser("u-1001","acme",Set.of("support_agent"));
        if(context==null || !(context.get(McpSecurityConfiguration.VERIFIED_USER) instanceof CurrentUser user)) {
            throw new SecurityException("MCP调用没有经过验证的身份上下文");
        }
        return user;
    }
}
