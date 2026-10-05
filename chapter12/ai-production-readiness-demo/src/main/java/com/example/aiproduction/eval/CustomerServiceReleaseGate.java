package com.example.aiproduction.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/**
 * 第 10.2 章真实接口的批量发布门禁。每条用例独立发请求、读取 AI Run 并写 NDJSON，
 * 不再拿同一个固定答案冒充所有题目的输出。只向专用评测实例运行，避免污染业务工单和会话。
 */
public class CustomerServiceReleaseGate {
    public record Case(String id, String customerId, String question, String orderNo,
                       String expectedTool, String expectedKeyword, boolean expectedHandoff,
                       String forbiddenKeyword, boolean redLine) { }
    public record Result(String id, boolean passed, boolean redLine, String requestId, String reason,
                         long latencyMs, String model, String promptVersion,
                         Long inputTokens, Long outputTokens) { }
    public record Report(List<Result> results, double passRate, long p95Ms, boolean releaseAllowed) { }
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json=new ObjectMapper();
    private final String baseUrl, token;
    public CustomerServiceReleaseGate(String baseUrl, String token) {
        this.baseUrl=baseUrl.replaceAll("/+$", ""); this.token=token;
    }
    public Report run(List<Case> cases, Path output, double requiredRate, long maxP95Ms) throws Exception {
        if(cases.isEmpty() || requiredRate<0 || requiredRate>1 || maxP95Ms<=0)
            throw new IllegalArgumentException("门禁需要非空评测集、合法通过率和延迟上限");
        if(cases.stream().map(Case::id).distinct().count()!=cases.size()) throw new IllegalArgumentException("用例 ID 必须唯一");
        Files.createDirectories(output.toAbsolutePath().getParent());
        List<Result> results=new ArrayList<>();
        // CREATE_NEW 防止重复运行覆盖之前的证据。每条结果立即刷盘，后续某条失败不会丢掉已完成记录。
        try(var writer=Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            for(Case test:cases) {
                Result result=execute(test); results.add(result);
                writer.write(json.writeValueAsString(result)); writer.newLine(); writer.flush();
            }
        }
        double rate=results.stream().filter(Result::passed).count()/(double)cases.size();
        List<Long> latencies=results.stream().map(Result::latencyMs).sorted().toList();
        long p95=latencies.get((int)Math.ceil(latencies.size()*0.95)-1);
        boolean redLineFailed=results.stream().anyMatch(r -> r.redLine() && !r.passed());
        Report report=new Report(List.copyOf(results), rate, p95, rate>=requiredRate && p95<=maxP95Ms && !redLineFailed);
        Files.writeString(Path.of(output+".summary.json"), json.writerWithDefaultPrettyPrinter().writeValueAsString(report),
            StandardOpenOption.CREATE_NEW);
        return report;
    }
    private Result execute(Case test) {
        long start=System.nanoTime(); String requestId="";
        try {
            Map<String,Object> payload=new LinkedHashMap<>();
            payload.put("sessionId", "EVAL-"+UUID.randomUUID()); payload.put("customerId",test.customerId());
            payload.put("question",test.question()); payload.put("orderNo",test.orderNo());
            JsonNode answer=send("/api/support/chat", json.writeValueAsString(payload));
            requestId=answer.path("requestId").asText();
            if(requestId.isBlank()) throw new IllegalStateException("响应缺少 requestId");
            JsonNode audit=send("/api/support/ai-runs/"+requestId,null);
            String text=answer.path("answer").asText();
            boolean tool=test.expectedTool().isBlank();
            for(JsonNode trace:answer.path("toolTraces")) tool |= test.expectedTool().equals(trace.path("toolName").asText());
            boolean forbidden=!test.forbiddenKeyword().isBlank() && text.contains(test.forbiddenKeyword());
            boolean passed=tool && text.contains(test.expectedKeyword()) && !forbidden
                && answer.path("handoffRequired").asBoolean()==test.expectedHandoff()
                && List.of("SUCCEEDED","CACHED").contains(audit.path("status").asText());
            return new Result(test.id(),passed,test.redLine(),requestId,
                "tool="+tool+", forbidden="+forbidden+", status="+audit.path("status").asText(),elapsed(start),
                audit.path("model").asText(),audit.path("promptVersion").asText(),
                count(audit,"inputTokens"),count(audit,"outputTokens"));
        } catch(Exception failure) {
            if(failure instanceof InterruptedException) Thread.currentThread().interrupt();
            // 失败也是评测结果，不打印响应正文和访问凭据，不把网络失败排除出通过率分母。
            return new Result(test.id(),false,test.redLine(),requestId,failure.getClass().getSimpleName(),
                elapsed(start),"","",null,null);
        }
    }
    private JsonNode send(String path,String body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create(baseUrl+path)).timeout(Duration.ofSeconds(45))
            .header("Accept","application/json");
        if(!token.isBlank()) builder.header("Authorization","Bearer "+token);
        if(body!=null) builder.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200) throw new IllegalStateException("客服接口 HTTP "+response.statusCode());
        return json.readTree(response.body());
    }
    private Long count(JsonNode root,String name) { return root.path(name).isIntegralNumber()?root.path(name).longValue():null; }
    private long elapsed(long start) { return (System.nanoTime()-start)/1_000_000; }
}
