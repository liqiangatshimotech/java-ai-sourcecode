package com.example.aicustomerservice.service;

import com.example.aicustomerservice.ai.*;
import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.dao.*;
import com.example.aicustomerservice.entity.*;
import com.example.aicustomerservice.security.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用受控 HTTP 服务验证真实适配器契约，不消耗外部模型额度。 */
class ProductionHardeningTest {
    private SupportPrompt prompt() {
        return new SupportPrompt("开票",new CustomerProfile("C1","张先生",CustomerTier.NORMAL,"***"),
            new SupportIntent(IntentType.FAQ,1,"测试"),List.of(),List.of(),false,List.of("上轮客户询问发票"));
    }
    @Test void realAdapterShouldSendHistoryAndReadActualUsage() throws Exception {
        AtomicReference<String> request=new AtomicReference<>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/chat/completions", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            byte[] data="{\"model\":\"returned-model\",\"choices\":[{\"message\":{\"content\":\"请提供发票抬头\"}}],\"usage\":{\"prompt_tokens\":17,\"completion_tokens\":8}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,data.length); exchange.getResponseBody().write(data); exchange.close();
        });
        server.start();
        try {
            var client=new OpenAiCompatibleChatModelClient(new ModelSettings(AiProvider.DEEPSEEK,
                "http://127.0.0.1:"+server.getAddress().getPort()+"/v1","test","deepseek-v4-flash",0));
            var result=client.generateCompletion(prompt());
            assertThat(result.inputTokens()).isEqualTo(17); assertThat(result.outputTokens()).isEqualTo(8);
            assertThat(result.actualModel()).isEqualTo("returned-model");
            assertThat(request.get()).contains("上轮客户询问发票");
        } finally { server.stop(0); }
    }
    @Test void embeddingShouldCallHttpAndRejectDimensionMismatch() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/embeddings", exchange -> {
            byte[] bytes="{\"data\":[{\"embedding\":[0.6,0.8]}]}".getBytes();
            exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        }); server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort()+"/v1";
            assertThat(new OpenAiCompatibleEmbeddingService(base,"","test-embed",2).embed("发票")).containsExactly(0.6f,0.8f);
            assertThatThrownBy(() -> new OpenAiCompatibleEmbeddingService(base,"","test-embed",3).embed("发票"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("维度");
        } finally { server.stop(0); }
    }
    @Test void busyRequestShouldRejectAndFailureShouldReleasePermit() throws Exception {
        ChatModelClient client=mock(ChatModelClient.class);
        CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
        when(client.generateCompletion(any())).thenAnswer(invocation -> {
            entered.countDown(); if(!release.await(3,TimeUnit.SECONDS)) throw new AssertionError("等待超时");
            throw new BusinessException("MODEL_CALL_FAILED","模拟失败");
        }).thenReturn(ModelCompletion.unmetered("恢复"));
        QueuedModelExecutor executor=new QueuedModelExecutor(client,1);
        try(var pool=Executors.newSingleThreadExecutor()) {
            Future<?> first=pool.submit(() -> executor.generate(prompt()));
            assertThat(entered.await(3,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> executor.generate(prompt())).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("MODEL_BUSY");
            release.countDown(); assertThatThrownBy(() -> first.get(3,TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
            assertThat(executor.generate(prompt()).answer()).isEqualTo("恢复");
        } finally { release.countDown(); }
    }
    @Test void cacheShouldExpireAndEvictLeastRecentlyUsedEntry() {
        AtomicReference<Instant> time=new AtomicReference<>(Instant.EPOCH);
        Clock clock=new Clock() {
            public ZoneId getZone(){ return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone){ return this; }
            public Instant instant(){ return time.get(); }
        };
        var cache=new InMemoryHotQuestionCache(clock,Duration.ofSeconds(10),2);
        cache.put("a","A");cache.put("b","B");cache.get("a");cache.put("c","C");
        assertThat(cache.get("b")).isEmpty(); assertThat(cache.get("a")).contains("A");
        time.set(Instant.EPOCH.plusSeconds(10));assertThat(cache.get("a")).isEmpty();
    }
    @Test void publishFailureMustStayUnpublishedAndRetrySameRequest() {
        var dao=new InMemoryKnowledgeDao(Clock.systemUTC());
        var vectors=mock(KnowledgeVectorDao.class);
        doThrow(new BusinessException("EMBEDDING_FAILED","故障")).doNothing().when(vectors).syncDocument(any(),anyList());
        var service=new KnowledgeLifecycleService(dao,new SemanticChunker(),Clock.systemUTC(),vectors);
        var draft=service.importDraft("故障恢复测试","退款必须人工审核。","v1","MANUAL","test","admin");
        assertThatThrownBy(() -> service.publish(draft.documentId(),"admin")).isInstanceOf(BusinessException.class);
        assertThat(service.get(draft.documentId()).status()).isEqualTo(KnowledgeStatus.DRAFT);
        assertThat(service.publish(draft.documentId(),"admin").status()).isEqualTo(KnowledgeStatus.PUBLISHED);
    }
    @Test void auditShouldRetainFailedAndRejectedRuns() {
        var service=new AiRunService(new InMemoryAiRunDao(Clock.systemUTC()));
        var settings=ModelSettings.fromProviderName("deepseek");
        service.begin("REQ-test","S-test",settings,Instant.EPOCH);
        assertThat(service.get("REQ-test").status()).isEqualTo("RUNNING");
        service.failed("REQ-test","S-test",settings,Instant.EPOCH,new BusinessException("MODEL_BUSY","busy"),5);
        assertThat(service.get("REQ-test").status()).isEqualTo("REJECTED");
        assertThat(service.get("REQ-test").inputTokens()).isNull();
    }
    @Test void verifiedIdentityShouldRejectSpoofingAndCrossTenantCredential() throws Exception {
        String identities="[{\"token\":\"abcdefghijklmnopqrstuvwx1234\",\"user\":\"verified-agent\",\"tenant\":\"tenant-a\",\"role\":\"agent\"}]";
        var auth=new SupportAuthentication(true,"tenant-a",identities);
        MockHttpServletRequest request=new MockHttpServletRequest("POST","/api/support/conversations/S1/reply");
        assertThatThrownBy(() -> auth.preHandle(request,new MockHttpServletResponse(),new Object())).isInstanceOf(BusinessException.class);
        request.addHeader("Authorization","Bearer abcdefghijklmnopqrstuvwx1234");
        assertThat(auth.preHandle(request,new MockHttpServletResponse(),new Object())).isTrue();
        assertThat(((SupportIdentity)request.getAttribute(SupportIdentity.ATTRIBUTE)).user()).isEqualTo("verified-agent");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
            new org.springframework.web.context.request.ServletRequestAttributes(request));
        try { assertThat(SupportIdentity.operator("spoofed-admin")).isEqualTo("verified-agent"); }
        finally { org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }
        request.setRequestURI("/api/support/conversations/S1/assign");
        assertThatThrownBy(() -> auth.preHandle(request,new MockHttpServletResponse(),new Object())).isInstanceOf(BusinessException.class);
        var other=new SupportAuthentication(true,"tenant-b",identities);
        assertThatThrownBy(() -> other.preHandle(request,new MockHttpServletResponse(),new Object())).isInstanceOf(BusinessException.class);
    }
}
