package com.example.langchain4jrag.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface PolicyAssistant {

    @SystemMessage("""
        你是企业客服政策助手。
        只能根据检索到的参考资料回答。
        如果资料不足，要明确说明不能确认，并建议升级人工处理。
        回答要包含处理步骤和需要回复给客户的关键信息。
        """)
    Result<String> answer(@UserMessage String question);
}
