package com.example.springaimcpserver;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 真实随机端口/mcp请求，签名与JWKS校验均执行；不mock SecurityContext或工具。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("remote")
class RemoteMcpSecurityTest {
    static final String ISSUER="http://127.0.0.1/test-issuer";
    static final String RESOURCE="http://127.0.0.1/course-mcp";
    static final RSAKey KEY;
    static final HttpServer JWKS;
    static {
        try {
            KEY=new RSAKeyGenerator(2048).keyID("test-key").generate();
            JWKS=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            JWKS.createContext("/jwks",exchange->{
                byte[] body=new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type","application/json");
                exchange.sendResponseHeaders(200,body.length);
                try(var stream=exchange.getResponseBody()){stream.write(body);}
            });JWKS.start();
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    @DynamicPropertySource static void props(DynamicPropertyRegistry r) {
        r.add("mcp.auth.issuer",()->ISSUER);r.add("mcp.auth.resource",()->RESOURCE);
        r.add("mcp.auth.jwk-set-uri",()->"http://127.0.0.1:"+JWKS.getAddress().getPort()+"/jwks");
    }
    @LocalServerPort int port;
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    String token(String issuer,String audience,String scope,String tenant,Instant expires,RSAKey signingKey)throws Exception {
        JWTClaimsSet claims=new JWTClaimsSet.Builder().issuer(issuer).subject("remote-user")
                .audience(audience).claim("scope",scope).claim("tenant_id",tenant)
                .issueTime(Date.from(Instant.now().minusSeconds(5))).expirationTime(Date.from(expires)).build();
        SignedJWT jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),claims);
        jwt.sign(new RSASSASigner(signingKey));return jwt.serialize();
    }
    String good(String tenant)throws Exception{return token(ISSUER,RESOURCE,"mcp.read",tenant,Instant.now().plusSeconds(300),KEY);}
    HttpResponse<String> call(String bearer,String order)throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/mcp"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type","application/json")
                .header("Accept","application/json, text/event-stream")
                .header("MCP-Protocol-Version","2025-06-18")
                .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"query_order_status\",\"arguments\":{\"orderId\":\""+order+"\",\"tenantId\":\"acme\",\"userId\":\"u-1001\"}}}"));
        if(bearer!=null)request.header("Authorization","Bearer "+bearer);
        return http.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void discoveryWorksWithoutTokenAnd401PointsToMetadata()throws Exception {
        var missing=call(null,"ORD-1001");assertEquals(401,missing.statusCode());
        assertTrue(missing.headers().firstValue("WWW-Authenticate").orElseThrow().contains("resource_metadata="));
        var metadata=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/.well-known/oauth-protected-resource")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,metadata.statusCode());assertTrue(metadata.body().contains(ISSUER));assertTrue(metadata.body().contains(RESOURCE));
    }
    @Test void verifiedIdentitySurvivesActualMcpDispatch()throws Exception {
        var response=call(good("acme"),"ORD-1001");
        assertEquals(200,response.statusCode(),response.body());
        assertTrue(response.body().contains("PAID"),response.body());
        assertFalse(response.body().contains("\"isError\":true"),response.body());
        var beta=call(good("beta"),"ORD-2001");
        assertEquals(200,beta.statusCode());
        assertTrue(beta.body().contains("REFUNDING"),beta.body());
    }
    @Test void wrongTenantCannotOverrideIdentityThroughToolArguments()throws Exception {
        var response=call(good("other-tenant"),"ORD-1001");
        assertEquals(200,response.statusCode(),response.body());
        assertTrue(response.body().contains("\"isError\":true") || response.body().contains("\"error\""),response.body());
        assertFalse(response.body().contains("PAID"),response.body());
        assertTrue(response.body().contains("无权读取该租户订单"),response.body());
    }
    @Test void expiredWrongAudienceWrongIssuerBadSignatureAre401()throws Exception {
        Instant future=Instant.now().plusSeconds(300);
        List<String> invalid=List.of(token(ISSUER,RESOURCE,"mcp.read","acme",Instant.now().minusSeconds(1),KEY),
                token(ISSUER,"https://wrong.example/mcp","mcp.read","acme",future,KEY),
                token(ISSUER,null,"mcp.read","acme",future,KEY),
                token("https://wrong.example",RESOURCE,"mcp.read","acme",future,KEY),
                token(ISSUER,RESOURCE,"mcp.read","acme",future,new RSAKeyGenerator(2048).generate()));
        for(String bearer:invalid)assertEquals(401,call(bearer,"ORD-1001").statusCode());
    }
    @Test void missingScopeIs403BeforeToolDispatch()throws Exception {
        String noScope=token(ISSUER,RESOURCE,"profile","acme",Instant.now().plusSeconds(300),KEY);
        assertEquals(403,call(noScope,"ORD-1001").statusCode());
    }
    @AfterAll static void stopJwks(){JWKS.stop(0);}
}
