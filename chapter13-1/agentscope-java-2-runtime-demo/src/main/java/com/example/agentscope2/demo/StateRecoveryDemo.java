package com.example.agentscope2.demo;

import com.example.agentscope2.state.VersionedAgentStateStore;
import com.example.agentscope2.state.VersionedAgentStateStore.Identity;
import com.example.agentscope2.config.CourseModelFactory;
import com.example.agentscope2.config.CourseModelSettings;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import reactor.core.publisher.Flux;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** 同一业务身份重建 Agent 和存储实例后，框架仍能加载已提交的历史。 */
public final class StateRecoveryDemo {
    public static String call(Path db,Identity identity,Model model,String question) {
        // identity 应由已认证主体产生，不能从未经校验的请求参数构造。
        var store=new VersionedAgentStateStore(db,identity);
        var agent=ReActAgent.builder().name("persistent-ticket-agent")
                .model(model).stateStore(store).maxIters(2).maxRetries(1)
                .sysPrompt("你是客服助手；沿用已保存的会话事实，不执行业务写操作。").build();
        var context=RuntimeContext.builder().userId(identity.user()).sessionId(identity.session()).build();
        var answer=agent.call(List.of(new UserMessage(question)),context).block(Duration.ofSeconds(60));
        if(answer==null) throw new IllegalStateException("模型未返回结果");
        // 必须提交成功后再把答复交给调用方；冲突时答复无效。
        store.commit();
        return answer.getTextContent();
    }
    /** 仅模拟模型最后一跳；ReActAgent 的加载、消息追加与保存均真实执行。 */
    public static final class OfflineModel implements Model {
        public volatile List<Msg> lastMessages=List.of();
        public String getModelName(){return "offline-state-test";}
        public Flux<ChatResponse> stream(List<Msg> messages,List<ToolSchema> tools,GenerateOptions options) {
            lastMessages=List.copyOf(messages);
            boolean remembered=messages.stream().anyMatch(m->m.getTextContent().contains("T-1001"));
            return Flux.just(ChatResponse.builder().content(List.of(TextBlock.builder()
                    .text(remembered?"[offline] 历史包含工单 T-1001":"[offline] 没有该工单历史").build())).build());
        }
    }
    public static void main(String[] args) {
        Path db=Path.of(System.getenv().getOrDefault("STATE_DB","target/state-recovery/course"));
        var identity=new Identity("tenant-a","alice","ticket-session");
        boolean live=Boolean.parseBoolean(System.getenv().getOrDefault("RUN_MODEL_CALL","false"));
        Model first=live?CourseModelFactory.create(CourseModelSettings.fromEnvironment()):new OfflineModel();
        System.out.println("mode="+(live?"live":"offline（真实框架状态恢复，模拟模型）"));
        System.out.println(call(db,identity,first,"请记住工单 T-1001"));
        // 第二次创建全新 Model/Agent/Store，读取同一数据库会话，而非复用对象内存。
        Model second=live?CourseModelFactory.create(CourseModelSettings.fromEnvironment()):new OfflineModel();
        System.out.println(call(db,identity,second,"刚才的工单编号是什么？"));
        System.out.println("已提交版本="+new VersionedAgentStateStore(db,identity).version());
        reactor.core.scheduler.Schedulers.shutdownNow();
    }
}
