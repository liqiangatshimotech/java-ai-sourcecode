package com.example.agentruntime.web;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
/** 真正启动随机端口 Web 容器，覆盖 Spring 注入和文章请求格式。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class AgentHttpTest {
    @Autowired TestRestTemplate http;
    @Test void articleHttpContractAndRepeatedApproval() {
        var first=http.postForEntity("/api/agent/runs",Map.of("userId","demo","goal","查询物流并申请补偿","orderNo","A20260001","allowWrite",false),Map.class);
        assertEquals(200,first.getStatusCode().value());assertEquals("WAITING_APPROVAL",first.getBody().get("status"));
        String id=first.getBody().get("runId").toString();
        assertEquals(200,http.getForEntity("/api/agent/runs/"+id+"/trace",Map.class).getStatusCode().value());
        assertEquals("COMPLETED",http.postForEntity("/api/agent/runs/"+id+"/approve",null,Map.class).getBody().get("status"));
        assertEquals(409,http.postForEntity("/api/agent/runs/"+id+"/approve",null,Map.class).getStatusCode().value());
        var stream=http.getForEntity("/api/agent/runs/"+id+"/events/stream",String.class);
        assertEquals(200,stream.getStatusCode().value());assertTrue(stream.getBody().contains("COMPLETED"));
    }
    @Test void missingOrderIsBadRequest() {
        assertEquals(400,http.postForEntity("/api/agent/runs",Map.of("userId","demo","goal","查询订单"),Map.class).getStatusCode().value());
    }
}
