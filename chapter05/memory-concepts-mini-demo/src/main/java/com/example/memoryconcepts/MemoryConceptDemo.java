package com.example.memoryconcepts;
import java.util.ArrayList;
import java.util.List;
/** 纯 Java 数据建模，不调用任何模型；原始消息和选出的上下文互不覆盖。 */
public class MemoryConceptDemo {
    public static void main(String[] args) {
        List<ChatTurn> history=new ArrayList<>();
        history.add(ChatTurn.user("我是华东区售后负责人，今天主要处理退款工单。"));
        history.add(ChatTurn.assistant("收到，后续建议围绕退款场景。"));
        history.add(ChatTurn.user("客户说退款三天还没到账，我应该先查什么？"));
        List<MemoryEntry> memory=List.of(
                new MemoryEntry("profile","用户自述负责华东区售后，不作为权限依据"),
                new MemoryEntry("task_context","当前对话围绕退款工单处理"));
        SupportSessionState state=new SupportSessionState("u-1001","华东区","T-20260609-001");
        System.out.println("配置（本例不调用模型）: "+ModelSettings.fromEnv());
        System.out.println("History 条数: "+history.size());
        System.out.println("Memory 条数: "+memory.size());
        System.out.println("业务状态: "+state);
    }
}
