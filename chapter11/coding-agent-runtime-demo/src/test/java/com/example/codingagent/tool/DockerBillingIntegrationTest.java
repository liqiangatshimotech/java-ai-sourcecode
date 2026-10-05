package com.example.codingagent.tool;

import com.example.codingagent.workspace.*;
import com.example.codingagent.runtime.CodingAgentRuntime;
import java.nio.file.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** 显式开启后运行真实 Docker：先证明旧业务代码失败，再证明修改后通过。 */
@EnabledIfEnvironmentVariable(named="RUN_DOCKER_TESTS",matches="true")
class DockerBillingIntegrationTest {
    @TempDir Path temp;
    @Test void compilesAndTestsOldAndNewBusinessCodeInContainer() throws Exception {
        Path root=DemoWorkspaceFactory.createFreshWorkspace(temp.resolve("coding-agent-container"));
        var context=new ToolContext(root,new WorkspaceBoundary(root),WorkspaceSnapshot.capture(root));
        var tool=new RunTestsTool();
        var before=tool.execute(context,Map.of());
        assertEquals("TESTS_FAILED",before.summary(),"必须实际执行旧代码并失败，不能把环境不可用当作反例通过");
        Path source=root.resolve(CodingAgentRuntime.TARGET_PATH);
        Files.writeString(source,Files.readString(source).replace("0.20","0.30"));
        var after=tool.execute(context,Map.of());
        assertTrue(after.success(),after.summary()+": "+after.output());
    }
}
