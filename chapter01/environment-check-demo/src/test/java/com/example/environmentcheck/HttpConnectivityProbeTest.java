package com.example.environmentcheck;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** 本机 HTTP fixture 覆盖供应商响应契约，不调用真实模型、不产生模型费用。 */
class HttpConnectivityProbeTest {
    HttpServer server;ModelConfig config;
    volatile int status=200;volatile long delay;
    volatile String body="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"OK\"}}]}";
    volatile String authorization, request;
    @BeforeEach void start()throws Exception{
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/chat/completions",exchange->{
            authorization=exchange.getRequestHeaders().getFirst("Authorization");
            request=new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            try{if(delay>0)Thread.sleep(delay);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
            byte[] data=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try{exchange.sendResponseHeaders(status,data.length);exchange.getResponseBody().write(data);}finally{exchange.close();}
        });server.start();
        config=ModelConfig.from(Map.of("AI_BASE_URL","http://127.0.0.1:"+server.getAddress().getPort(),"DEEPSEEK_API_KEY","test-only-key"));
    }
    @AfterEach void stop(){server.stop(0);}
    CheckResult probe(){return new HttpConnectivityProbe(Duration.ofSeconds(2)).probe(config);}
    @Test void validatesSuccessWithoutPrintingGeneratedTextOrKey(){
        var result=probe();assertEquals(CheckResult.Status.PASS,result.status());assertEquals("Bearer test-only-key",authorization);
        assertFalse(request.contains("test-only-key"));assertTrue(request.contains("\"max_tokens\":16"));assertFalse(result.toString().contains("test-only-key"));
    }
    @Test void classifiesAuthenticationAndDoesNotEchoErrorBody(){status=401;body="sensitive-error-body";var result=probe();assertTrue(result.detail().startsWith("AUTH_FAILED"));assertFalse(result.toString().contains(body));}
    @Test void classifiesRateLimit(){status=429;assertTrue(probe().detail().startsWith("RATE_LIMITED"));}
    @Test void classifiesUnsupportedModel(){status=404;body="{\"error\":{\"code\":\"model_not_found\",\"message\":\"sensitive\"}}";assertTrue(probe().detail().startsWith("MODEL_UNSUPPORTED"));}
    @Test void wrongEndpointIsNotMisreportedAsUnsupportedModel(){status=404;body="not found";assertTrue(probe().detail().startsWith("HTTP_ERROR"));}
    @Test void classifiesTimeout(){delay=200;assertTrue(new HttpConnectivityProbe(Duration.ofMillis(30)).probe(config).detail().startsWith("TIMEOUT"));}
    @Test void refusesTruncatedCompletion(){body="{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"unfinished\"}}]}";assertTrue(probe().detail().startsWith("TRUNCATED"));}
    @Test void invalidJsonDoesNotPass(){body="not JSON";assertEquals(CheckResult.Status.FAIL,probe().status());}
    @Test void oversizedBodyDoesNotPass(){body="x".repeat(65_537);assertEquals(CheckResult.Status.FAIL,probe().status());}
    @Test void missingTextDoesNotPass(){body="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{}}]}";assertTrue(probe().detail().startsWith("INVALID_RESPONSE"));}
    // Ollama 等兼容服务可能没有 error.code，仍需识别固定模型错误语义并隐藏原文。
    @Test void recognizesLocalModelNotFoundMessage(){status=404;body="{\"error\":\"model 'private-name' not found\"}";var result=probe();assertTrue(result.detail().startsWith("MODEL_UNSUPPORTED"));assertFalse(result.toString().contains("private-name"));}
    @Test void refusalDoesNotPass(){body="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"refusal\":\"private refusal\",\"content\":\"\"}}]}";assertTrue(probe().detail().startsWith("REFUSAL"));}
}
