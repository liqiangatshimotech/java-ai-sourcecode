package com.example.codingagent;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.model.*;
import com.example.codingagent.service.*;
import com.example.codingagent.tool.*;
import com.example.codingagent.runtime.CodingAgentRuntime;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** 服务状态机测试使用明确的容器替身，只验证审批/幂等，不代表实际编译已通过。 */
class JobApprovalTest {
    @TempDir Path temp;
    AtomicInteger modelCalls=new AtomicInteger();
    AtomicInteger testCalls=new AtomicInteger();
    private CodingAgentJobService service(boolean pass) {
        CodingModel model=(task,path,current,settings)-> {modelCalls.incrementAndGet();return new RuleBasedCodingModel().proposeChange(task,path,current,settings);};
        AgentTool tests=new AgentTool(){
            public String name(){return "run_tests";} public ToolRisk risk(){return ToolRisk.MEDIUM;}
            public ToolResult execute(ToolContext c,Map<String,String>a){testCalls.incrementAndGet();return pass?ToolResult.success("TESTS_PASSED","test double"):ToolResult.failure("TESTS_FAILED","test double: expected 70.00");}
        };
        var registry=new ToolRegistry().register(new WriteFileTool()).register(tests).register(new GitDiffTool());
        return new CodingAgentJobService(temp,model,registry,ModelSettings.from(Map.of("AI_PROVIDER","deepseek")));
    }
    private CodingAgentJob create(CodingAgentJobService service)throws Exception{return service.createJob("billing","feature","把会员折扣上限从 20% 调整为 30%","test");}
    private void approve(CodingAgentJobService service,CodingAgentJob job)throws Exception{
        service.approve(job.taskId(),job.prepared().proposalId(),job.prepared().sourceVersion(),"reviewer","approved exact diff");
    }
    @Test void creationPreviewsWithoutWritingAndExecutionDoesNotRegenerate()throws Exception{
        var service=service(true);var job=create(service);
        assertEquals(CodingTaskStatus.WAITING_APPROVAL,job.status());assertEquals(1,modelCalls.get());
        assertTrue(job.prepared().diff().contains("0.30"));
        assertTrue(Files.readString(job.workspaceRoot().resolve(CodingAgentRuntime.TARGET_PATH)).contains("0.20"));
        approve(service,job);service.run(job.taskId(),job.prepared().proposalId());
        assertEquals(1,modelCalls.get());assertEquals(1,testCalls.get());assertEquals(CodingTaskStatus.COMPLETED,job.status());
    }
    @Test void repeatedAndConcurrentRunsExecuteOnlyOnce()throws Exception{
        var service=service(true);var job=create(service);approve(service,job);String id=job.prepared().proposalId();
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->service.run(job.taskId(),id));var b=pool.submit(()->service.run(job.taskId(),id));a.get();b.get();
        }
        service.run(job.taskId(),id);assertEquals(1,testCalls.get());
    }
    @Test void approvalRejectsStaleProposalAndSourceVersion()throws Exception{
        var service=service(true);var job=create(service);
        assertThrows(IllegalStateException.class,()->service.approve(job.taskId(),"stale",job.prepared().sourceVersion(),"x","x"));
        Files.writeString(job.workspaceRoot().resolve(CodingAgentRuntime.TARGET_PATH),"changed after review");
        assertThrows(IllegalStateException.class,()->approve(service,job));assertEquals(0,testCalls.get());
    }
    @Test void executionRejectsSourceChangedAfterApproval()throws Exception{
        var service=service(true);var job=create(service);approve(service,job);
        Files.writeString(job.workspaceRoot().resolve(CodingAgentRuntime.TARGET_PATH),"changed after approval");
        service.run(job.taskId(),job.prepared().proposalId());
        assertEquals(CodingTaskStatus.FAILED,job.status());assertEquals(0,testCalls.get());
    }
    @Test void repairCreatesNewApprovalAndOldRunCannotExecuteIt()throws Exception{
        var service=service(false);var job=create(service);approve(service,job);String first=job.prepared().proposalId();
        service.run(job.taskId(),first);assertEquals(CodingTaskStatus.FAILED,job.status());
        service.repair(job.taskId(),first);assertEquals(CodingTaskStatus.WAITING_APPROVAL,job.status());
        assertNull(job.approvalDecision());assertNotEquals(first,job.prepared().proposalId());
        service.run(job.taskId(),first);assertEquals(1,testCalls.get());
        assertThrows(IllegalStateException.class,()->service.run(job.taskId(),job.prepared().proposalId()));
        approve(service,job);service.run(job.taskId(),job.prepared().proposalId());
        assertThrows(IllegalStateException.class,()->service.repair(job.taskId(),job.prepared().proposalId()));
    }
}
