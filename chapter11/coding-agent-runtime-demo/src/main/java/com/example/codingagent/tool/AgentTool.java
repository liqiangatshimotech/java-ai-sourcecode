package com.example.codingagent.tool;

import java.io.IOException;
import java.util.Map;

public interface AgentTool {

    String name();

    ToolRisk risk();

    ToolResult execute(ToolContext context, Map<String, String> arguments) throws IOException;
}
