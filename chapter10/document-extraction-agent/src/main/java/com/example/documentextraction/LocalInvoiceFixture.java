package com.example.documentextraction;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** 合成票据与本地HTTP协议夹具，不读取或识别真实发票，不是OCR引擎。 */
public final class LocalInvoiceFixture implements AutoCloseable {
    public static final String TEXT="""
            合成测试单据，不可报销
            发票号码 DEMO-20260909
            开票日期 2026-09-09
            销售方 示例商店
            购买方 示例公司
            币种 CNY
            商品 咨询服务 数量 2 单价 50.00 金额 100.00
            未税金额 100.00
            税额 6.00
            价税合计 106.00
            """;
    public static String candidate()throws Exception {
        var root=ExtractionClient.JSON.createObjectNode();
        Map<String,String> values=new LinkedHashMap<>();
        values.put("invoiceNumber","DEMO-20260909");values.put("issueDate","2026-09-09");
        values.put("seller","示例商店");values.put("buyer","示例公司");values.put("currency","CNY");
        values.put("netAmount","100.00");values.put("taxAmount","6.00");values.put("totalAmount","106.00");
        var evidence=root.putObject("evidence");
        values.forEach((key,value)->{root.put(key,value);evidence.putObject(key).put("page",1).putNull("bbox").put("quote",value);});
        var line=root.putArray("items").addObject();
        line.put("description","咨询服务").put("quantity","2").put("unitPrice","50.00").put("amount","100.00");
        line.putObject("evidence").put("page",1).putNull("bbox").put("quote","咨询服务 数量 2 单价 50.00 金额 100.00");
        return root.toString();
    }
    public static String envelope(String candidate,String finish)throws Exception {
        return ExtractionClient.JSON.writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason",finish,
                "message",Map.of("role","assistant","content",candidate)))));
    }
    private final HttpServer server;
    private final ExecutorService executor=Executors.newCachedThreadPool();
    public volatile String lastRequest="";
    public volatile String lastAuthorization="";
    public volatile String response;
    public volatile int status=200;
    public volatile long delayMillis;
    public final java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
    public LocalInvoiceFixture()throws Exception {
        response=envelope(candidate(),"stop");
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(executor);
        server.createContext("/v1/chat/completions",exchange->{
            calls.incrementAndGet();lastRequest=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            lastAuthorization=exchange.getRequestHeaders().getFirst("Authorization");
            try {if(delayMillis>0)Thread.sleep(delayMillis);}catch(InterruptedException e){Thread.currentThread().interrupt();}
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");
            try {exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);}finally{exchange.close();}
        });server.start();
    }
    public URI baseUrl(){return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v1");}
    public ModelSettings settings(){return new ModelSettings("deepseek",baseUrl(),"fixture-not-ocr","fixture-only",true,false,"");}
    public void close(){server.stop(0);executor.shutdownNow();}
}
