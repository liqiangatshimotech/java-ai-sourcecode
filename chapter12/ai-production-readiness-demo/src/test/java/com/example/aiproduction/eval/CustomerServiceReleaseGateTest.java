package com.example.aiproduction.eval;

import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceReleaseGateTest {
    @TempDir Path output;
    @Test void shouldExecuteEachQuestionPersistResultsAndBlockAnyRedLineFailure() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/support/chat",exchange -> {
            new ObjectMapper().readTree(exchange.getRequestBody()); // 确认请求是有效 JSON。
            int number=calls.incrementAndGet();
            byte[] body=("{\"requestId\":\"REQ-"+number+"\",\"answer\":\"发票退款已完成\",\"handoffRequired\":false,\"toolTraces\":[]}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });
        server.createContext("/api/support/ai-runs",exchange -> {
            byte[] body="{\"status\":\"SUCCEEDED\",\"model\":\"test-model\",\"promptVersion\":\"v2\",\"inputTokens\":12,\"outputTokens\":8}".getBytes();
            exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();
        try {
            var gate=new CustomerServiceReleaseGate("http://127.0.0.1:"+server.getAddress().getPort(),"");
            var cases=List.of(
                new CustomerServiceReleaseGate.Case("normal","C1","发票",null,"","发票",false,"",false),
                new CustomerServiceReleaseGate.Case("red","C1","退款",null,"","退款",false,"退款已完成",true));
            Path file=output.resolve("batch.ndjson");
            var report=gate.run(cases,file,0.5,10000);
            assertEquals(2,calls.get()); assertEquals(0.5,report.passRate()); assertFalse(report.releaseAllowed());
            assertEquals(2,Files.readAllLines(file).size());assertTrue(Files.exists(Path.of(file+".summary.json")));
            assertEquals(12L,report.results().getFirst().inputTokens());
            assertThrows(FileAlreadyExistsException.class,() -> gate.run(cases,file,0.5,10000));
        } finally {server.stop(0);}
    }
    @Test void connectionFailureMustCountAsFailureInsteadOfBeingDropped() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        int port=server.getAddress().getPort();server.stop(0);
        var gate=new CustomerServiceReleaseGate("http://127.0.0.1:"+port,"");
        var report=gate.run(List.of(new CustomerServiceReleaseGate.Case("failure","C1","test",null,"","test",false,"",true)),
            output.resolve("failed.ndjson"),1,10000);
        assertEquals(0,report.passRate());assertFalse(report.releaseAllowed());
        assertNull(report.results().getFirst().inputTokens());
    }
}
