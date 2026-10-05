package com.example.codingagent.tool;

import com.example.codingagent.workspace.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RunTestsToolTest {
    @TempDir Path temp;
    private ToolContext context() throws Exception {
        Path root=DemoWorkspaceFactory.createFreshWorkspace(temp.resolve("coding-agent-unit"));
        return new ToolContext(root,new WorkspaceBoundary(root),WorkspaceSnapshot.capture(root));
    }
    @Test void fixedCommandHasNoNetworkReadOnlyMountAndResourceBudget() {
        List<String> command=RunTestsTool.command(Path.of("/tmp/input"),"container");
        for(String required:List.of("--network=none","--read-only","--cap-drop=ALL","--security-opt=no-new-privileges","--memory=512m","--pids-limit=64","--pull=never")) assertTrue(command.contains(required));
        assertTrue(command.stream().anyMatch(x->x.endsWith("dst=/src,readonly")));
        assertTrue(command.getLast().contains("javac -proc:none"));
    }
    @Test void environmentFailureIsNotTestSuccess() throws Exception {
        var tool=new RunTestsTool((command,timeout,limit)->new ContainerRunner.Result(125,"daemon absent",false,false));
        var result=tool.execute(context(),Map.of());
        assertFalse(result.success());assertEquals("TEST_ENVIRONMENT_UNAVAILABLE",result.summary());
    }
    @Test void outputOverflowNeverPassesEvenWithMarker() throws Exception {
        var tool=new RunTestsTool((command,timeout,limit)->new ContainerRunner.Result(0,"ALL_BILLING_TESTS_PASSED",false,true));
        assertFalse(tool.execute(context(),Map.of()).success());
    }
    @Test void timeoutNeverPassesEvenWithMarker() throws Exception {
        var tool=new RunTestsTool((command,timeout,limit)->new ContainerRunner.Result(0,"ALL_BILLING_TESTS_PASSED",true,false));
        assertFalse(tool.execute(context(),Map.of()).success());
    }
    @Test void actualPipeReaderHasHardOutputBound() throws Exception {
        // 只执行受信任的 JDK 版本命令，绝不在宿主加载模型生成的代码。
        var result=new BoundedProcessRunner().run(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-version"),Duration.ofSeconds(5),8);
        assertTrue(result.outputExceeded());assertTrue(result.output().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=8);
    }
}
