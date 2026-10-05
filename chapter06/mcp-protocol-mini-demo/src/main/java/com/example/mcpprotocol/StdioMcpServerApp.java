package com.example.mcpprotocol;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 教学协议子集的真实 stdio 入口。一行请求对应至多一行响应，通知不写响应。
 * stdout 只承载 JSON-RPC，启动和错误诊断写 stderr，不混入普通日志。
 * 完整远程 MCP、认证与传输使用 6.2 的官方框架示例。
 */
public final class StdioMcpServerApp {
    public static void main(String[] args) throws IOException {
        serve(new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
    }

    static void serve(Reader input, Writer output) throws IOException {
        MiniMcpServer server = MiniMcpServer.demoServer();
        PrintWriter writer = new PrintWriter(output, true);
        BufferedReader reader = new BufferedReader(input);
        StringBuilder line = new StringBuilder();
        boolean oversized = false;
        int value;
        while ((value = reader.read()) != -1) {
            if (value == '\n') {
                respond(server, writer, line, oversized);
                line.setLength(0); oversized = false;
            } else if (value != '\r' && !oversized) {
                if (line.length() >= 1_048_576) { oversized = true; line.setLength(0); }
                else line.append((char) value);
            }
        }
        if (!line.isEmpty() || oversized) respond(server, writer, line, oversized);
    }

    private static void respond(MiniMcpServer server, PrintWriter writer, StringBuilder line, boolean oversized) {
        if (oversized) {
            writer.println("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Request too large\"}}");
        } else if (!line.toString().isBlank()) server.handleLine(line.toString()).ifPresent(writer::println);
    }
}
