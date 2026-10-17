package com.example.springaipersistentmemory.memory;
import java.util.Map;
/** 只接受明确的固定偏好表达，不把用户任意文本当系统规则，不推断未表达的偏好。 */
public class PreferenceExtractor {
    public Map<String,String> extract(String input) {
        if(input==null) return Map.of();
        String text=input.strip().replace("。","").replace("，",",");
        return switch(text) {
            case "以后回答用中文","以后请用中文回答" -> Map.of("language","回答用中文");
            case "以后回答短一点","以后回答简短一点","以后回答别太长" -> Map.of("style","回答保持简短");
            case "以后回答用中文,别太长","以后回答用中文,尽量短一点" ->
                    Map.of("language","回答用中文","style","回答保持简短");
            default -> Map.of(); // 不匹配不代表恶意，只表示本例没有得到可自动确认的偏好。
        };
    }
}
