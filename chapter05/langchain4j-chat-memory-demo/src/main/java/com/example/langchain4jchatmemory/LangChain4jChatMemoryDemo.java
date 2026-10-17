package com.example.langchain4jchatmemory;
import com.example.langchain4jmemory.assistant.AssistantFactory;
import com.example.langchain4jmemory.config.ModelFactory;
import com.example.langchain4jmemory.demo.MemoryIsolationDemo;
import com.example.langchain4jmemory.demo.WindowComparisonDemo;
/** 默认真实模型；--windows 只比较窗口，无需 API Key。 */
public class LangChain4jChatMemoryDemo {
    public static void main(String[] args) {
        if (args.length==1 && args[0].equals("--windows")) { WindowComparisonDemo.main(args); return; }
        if(args.length!=0) throw new IllegalArgumentException("只支持无参数或 --windows");
        new MemoryIsolationDemo(new AssistantFactory().create(ModelFactory.fromEnvironment())).run();
    }
}
