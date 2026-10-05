package com.example.springaimcpclient.demo;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Set;

/**
 * 远程授权联调入口：使用已经由外部IdP签发、audience指向当前MCP资源的访问令牌。
 * 此探针不启动Spring应用、不调用大模型，也不实现授权码/PKCE与刷新令牌流程。
 */
public final class RemoteMcpProbe {
    public static void main(String[] args)throws Exception {
        URI endpoint=URI.create(System.getenv().getOrDefault("MCP_SERVER_URL","http://127.0.0.1:8082/mcp"));
        if(!endpoint.isAbsolute() || endpoint.getHost()==null || endpoint.getUserInfo()!=null
                || !(endpoint.getScheme().equals("https") || (endpoint.getScheme().equals("http")
                && Set.of("127.0.0.1","localhost","[::1]").contains(endpoint.getHost())))) {
            throw new IllegalArgumentException("使用HTTPS；HTTP仅允许本机联调");
        }
        String token=System.getenv().getOrDefault("MCP_ACCESS_TOKEN","");
        // 不跟随重定向，避免Bearer被发给另一个目标；token永远不打印。
        HttpClient http=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5)).build();
        var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(15))
                .header("Content-Type","application/json").header("Accept","application/json, text/event-stream")
                .header("MCP-Protocol-Version","2025-06-18")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
                        """));
        if(!token.isBlank()) request.header("Authorization","Bearer "+token);
        var response=http.send(request.build(),HttpResponse.BodyHandlers.ofString());
        System.out.println("HTTP "+response.statusCode());
        if(response.statusCode()==401) {
            System.out.println("发现授权入口："+response.headers().firstValue("WWW-Authenticate").orElse("未提供发现信息"));
            System.out.println("请通过外部IdP完成授权码+PKCE，并为本MCP资源申请mcp.read后重试。");
        } else if(response.statusCode()==403) {
            System.out.println("访问令牌缺少所需scope，不能改工具参数绕过授权。");
        } else if(response.statusCode()==200) {
            System.out.println(response.body());
        } else {
            throw new IllegalStateException("MCP连接失败，HTTP "+response.statusCode());
        }
    }
}
