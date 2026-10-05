package com.example.agentscope2.state;
import com.example.agentscope2.demo.StateRecoveryDemo;
import com.example.agentscope2.state.VersionedAgentStateStore.Identity;
import io.agentscope.core.state.AgentState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class StateRecoveryTest {
    @TempDir Path dir;
    private final Identity id=new Identity("a","u","s");
    private VersionedAgentStateStore store(Identity i){return new VersionedAgentStateStore(dir.resolve("state"),i);}
    @Test void realAgentReloadsMessagesAfterInstanceReconstruction() {
        StateRecoveryDemo.call(dir.resolve("state"),id,new StateRecoveryDemo.OfflineModel(),"记住工单 T-1001");
        var model=new StateRecoveryDemo.OfflineModel();
        assertTrue(StateRecoveryDemo.call(dir.resolve("state"),id,model,"工单号是什么").contains("T-1001"));
        assertTrue(model.lastMessages.stream().anyMatch(m->m.getTextContent().contains("记住工单 T-1001")));
        assertEquals(2,store(id).version());
    }
    @Test void tenantUserAndSessionAreIsolated() {
        StateRecoveryDemo.call(dir.resolve("state"),id,new StateRecoveryDemo.OfflineModel(),"记住工单 T-1001");
        for(var other:List.of(new Identity("b","u","s"),new Identity("a","v","s"),new Identity("a","u","t"))) {
            var model=new StateRecoveryDemo.OfflineModel();
            assertFalse(StateRecoveryDemo.call(dir.resolve("state"),other,model,"查看历史").contains("T-1001"));
        }
        assertThrows(SecurityException.class,()->store(id).exists("v","s"));
    }
    @Test void concurrentWritersHaveExactlyOneWinner() throws Exception {
        var one=store(id);var two=store(id);
        one.save("u","s","state",AgentState.builder().userId("u").sessionId("s").build());
        two.save("u","s","state",AgentState.builder().userId("u").sessionId("s").build());
        CountDownLatch start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var results=List.of(one,two).stream().map(s->pool.submit(()->{
                start.await();try{s.commit();return true;}catch(VersionedAgentStateStore.VersionConflict e){return false;}
            })).toList();start.countDown();
            assertNotEquals(results.get(0).get(),results.get(1).get());
        }
        assertEquals(1,store(id).version());
    }
    @Test void deletionPersistsAndStaleWriterCannotResurrectState() {
        StateRecoveryDemo.call(dir.resolve("state"),id,new StateRecoveryDemo.OfflineModel(),"记住工单 T-1001");
        var stale=store(id);var deleting=store(id);
        deleting.delete("u","s");deleting.commit();
        assertFalse(store(id).exists("u","s"));
        assertTrue(store(id).listSessionIds("u").isEmpty());
        assertThrows(VersionedAgentStateStore.VersionConflict.class,stale::commit);
        assertFalse(store(id).exists("u","s"));
        var model=new StateRecoveryDemo.OfflineModel();
        assertFalse(StateRecoveryDemo.call(dir.resolve("state"),id,model,"查看历史").contains("T-1001"));
    }
}
