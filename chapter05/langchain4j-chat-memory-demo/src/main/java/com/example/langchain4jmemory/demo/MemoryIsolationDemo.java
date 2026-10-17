package com.example.langchain4jmemory.demo;
import com.example.langchain4jmemory.assistant.Assistant;
/** 两个 ID 交替提问；同一 ID 串行调用，防止同时覆盖窗口。 */
public class MemoryIsolationDemo {
    private final Assistant assistant;
    public MemoryIsolationDemo(Assistant assistant) { this.assistant=assistant; }
    public void run() {
        assistant.chat("user-a","我叫老李，负责华东区售后。");
        assistant.chat("user-b","我叫小周，负责华南区售前。");
        System.out.println("user-a: "+assistant.chat("user-a","我负责哪个区域？"));
        System.out.println("user-b: "+assistant.chat("user-b","我负责哪个区域？"));
    }
}
