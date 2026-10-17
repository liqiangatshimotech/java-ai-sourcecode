package com.example.memoryservice.memory;
import org.springframework.stereotype.Component;
/** 本例使用有限白名单；不匹配就不自动保存，这不是通用的 Prompt 注入检测器。 */
@Component
public class MemoryExtractor {
    public enum Source { USER, RAG, ASSISTANT }
    public MemoryDecision extract(String input) { return extract(input,Source.USER); }
    public MemoryDecision extract(String input,Source source) {
        if(source!=Source.USER) return MemoryDecision.skip("检索资料和模型回复不是用户偏好声明");
        if(input==null||input.isBlank()) return MemoryDecision.skip("没有输入");
        String text=input.strip().replace("。","").replace("，",",");
        // 业务事实始终回业务系统；包含这些词的整段输入不会保存为偏好。
        if(text.contains("订单")||text.contains("退款状态")) return MemoryDecision.skip("实时业务数据不进入长期记忆");
        return switch(text) {
            case "以后回答用中文" -> MemoryDecision.remember("language","回答用中文","明确的语言偏好");
            case "以后回答短一点","以后回答简短一点" -> MemoryDecision.remember("style","回答保持简短","明确的长度偏好");
            case "以后回答用中文,尽量短一点","以后回答用中文,别太长" ->
                    MemoryDecision.remember("response","回答用中文，保持简短","同时表达的两项偏好");
            case "我负责华东区售后" -> MemoryDecision.remember("profile","用户自述负责华东区售后","用户自述，不作为权限依据");
            default -> MemoryDecision.skip("不属于本例可自动确认的表达，需要另行审核");
        };
    }
}
