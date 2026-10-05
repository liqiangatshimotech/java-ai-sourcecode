package com.example.a2a.server;

import com.example.a2a.protocol.A2aJson;
import com.example.a2a.protocol.AgentCard;
import com.example.a2a.protocol.SendMessageRequest;
import com.example.a2a.protocol.SendMessageResponse;
import com.example.a2a.protocol.Task;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * 纯 JDK 实现的最小 A2A HTTP+JSON 服务。
 */
public class A2aHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final InMemoryTaskStore taskStore;
    private final CustomerSupportAgentExecutor executor;
    private URI baseUri;

    public A2aHttpServer(String host, int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.taskStore = new InMemoryTaskStore();
        this.executor = new CustomerSupportAgentExecutor(taskStore);
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        registerHandlers();
    }

    public void start() {
        server.start();
        InetSocketAddress address = server.getAddress();
        baseUri = URI.create("http://" + address.getHostString() + ":" + address.getPort());
    }

    public URI baseUri() {
        return baseUri;
    }

    private void registerHandlers() {
        server.createContext("/.well-known/agent-card.json", this::handleAgentCard);
        server.createContext("/message:send", this::handleSendMessage);
        server.createContext("/tasks", this::handleTasks);
    }

    private void handleAgentCard(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            writeProblem(exchange, 405, "METHOD_NOT_ALLOWED", "Agent Card only supports GET", null);
            return;
        }
        AgentCard card = CustomerSupportAgentCards.create(baseUri);
        writeJson(exchange, 200, card);
    }

    private void handleSendMessage(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeProblem(exchange, 405, "METHOD_NOT_ALLOWED", "message:send only supports POST", null);
            return;
        }
        if (!acceptsVersion(exchange)) {
            writeProblem(exchange, 400, "VERSION_NOT_SUPPORTED", "Only A2A-Version 1.0 is supported by this demo", null);
            return;
        }

        try (InputStream body = exchange.getRequestBody()) {
            SendMessageRequest request = A2aJson.MAPPER.readValue(body, SendMessageRequest.class);
            Task task = executor.handle(request);
            writeJson(exchange, 200, SendMessageResponse.task(task));
        } catch (IllegalArgumentException e) {
            writeProblem(exchange, 400, "INVALID_REQUEST", e.getMessage(), null);
        }
    }

    private void handleTasks(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            writeProblem(exchange, 405, "METHOD_NOT_ALLOWED", "tasks only supports GET in this demo", null);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String prefix = "/tasks/";
        if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
            writeProblem(exchange, 400, "INVALID_TASK_PATH", "Use /tasks/{taskId}", null);
            return;
        }

        String taskId = path.substring(prefix.length());
        Optional<Task> task = taskStore.findById(taskId);
        if (task.isEmpty()) {
            writeProblem(exchange, 404, "TASK_NOT_FOUND", "Task not found: " + taskId, Map.of("taskId", taskId));
            return;
        }
        writeJson(exchange, 200, task.get());
    }

    private boolean acceptsVersion(HttpExchange exchange) {
        String version = exchange.getRequestHeaders().getFirst("A2A-Version");
        return version == null || version.isBlank() || "1.0".equals(version);
    }

    private static void writeJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] bytes = A2aJson.MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/a2a+json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void writeProblem(
            HttpExchange exchange,
            int status,
            String reason,
            String detail,
            Map<String, String> metadata)
            throws IOException {
        Map<String, Object> errorInfo = metadata == null
                ? Map.of(
                        "@type", "type.googleapis.com/google.rpc.ErrorInfo",
                        "reason", reason,
                        "domain", "a2a-protocol.org")
                : Map.of(
                        "@type", "type.googleapis.com/google.rpc.ErrorInfo",
                        "reason", reason,
                        "domain", "a2a-protocol.org",
                        "metadata", metadata);
        byte[] bytes = A2aJson.MAPPER.writeValueAsBytes(Map.of(
                "error",
                Map.of(
                        "code", status,
                        "status", httpStatusName(status),
                        "message", detail,
                        "details", java.util.List.of(errorInfo))));
        exchange.getResponseHeaders().set("Content-Type", "application/a2a+json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String httpStatusName(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            default -> "ERROR";
        };
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
