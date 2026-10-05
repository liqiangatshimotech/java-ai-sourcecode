package com.example.documentextraction;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class DocumentExtractionTest {
    @TempDir Path temp;
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-09-09T00:00:00Z"),ZoneOffset.UTC);
    private final DocumentInput text=DocumentInput.text(LocalInvoiceFixture.TEXT);
    ObjectNode bill()throws Exception{return (ObjectNode)ExtractionClient.JSON.readTree(LocalInvoiceFixture.candidate());}
    ReviewResult decode(String json){return new ExtractionClient(ModelSettings.fromEnvironment(Map.of()),CLOCK).decode(json,text,"offline-test");}
    void has(ReviewResult result,String code){assertTrue(result.issues().stream().anyMatch(i->i.code().equals(code)),result.toString());}
    @Test void localHttpContractAndBigDecimalResult()throws Exception {
        try(var fixture=new LocalInvoiceFixture()) {
            ReviewResult result=new ExtractionClient(fixture.settings(),CLOCK).extract(text);
            assertEquals(ReviewResult.Status.READY_FOR_HUMAN_REVIEW,result.status());
            assertEquals(new BigDecimal("106.00"),result.candidate().totalAmount());
            var request=ExtractionClient.JSON.readTree(fixture.lastRequest);
            assertEquals("fixture-not-ocr",request.path("model").asText());assertFalse(request.path("stream").asBoolean());
            assertEquals("json_object",request.path("response_format").path("type").asText());
            assertEquals("disabled",request.path("thinking").path("type").asText());
            assertTrue(request.path("messages").get(1).path("content").asText().contains("合成测试单据"));
            assertEquals("Bearer fixture-only",fixture.lastAuthorization);
        }
    }
    @Test void disabledCallNeverSendsDocument()throws Exception {
        try(var fixture=new LocalInvoiceFixture()) {
            var config=new ModelSettings("deepseek",fixture.baseUrl(),"model","key",false,false,"");
            has(new ExtractionClient(config,CLOCK).extract(text),"MODEL_CALL_DISABLED");assertEquals(0,fixture.calls.get());
        }
    }
    @Test void defaultsAndThreeProvidersAreExplicit() {
        var defaults=ModelSettings.fromEnvironment(Map.of());assertEquals("deepseek",defaults.provider());assertFalse(defaults.runModel());assertFalse(defaults.visionEnabled());
        assertEquals("openai",ModelSettings.fromEnvironment(Map.of("AI_PROVIDER","chatgpt")).provider());
        assertEquals("ollama",ModelSettings.fromEnvironment(Map.of("AI_PROVIDER","ollama")).provider());
        assertThrows(IllegalArgumentException.class,()->ModelSettings.fromEnvironment(Map.of("AI_PROVIDER","unknown")));
    }
    @Test void missingFieldsAndStrictJsonNeverPass()throws Exception {
        var bill=bill();bill.remove("seller");has(decode(bill.toString()),"MISSING_FIELD");
        bill=bill();bill.put("paymentApproved",true);has(decode(bill.toString()),"INVALID_INVOICE_JSON");
        has(decode("```json\n{}\n```"),"INVALID_INVOICE_JSON");
        has(decode("{\"seller\":\"a\",\"seller\":\"b\"}"),"INVALID_INVOICE_JSON");
    }
    @Test void impossibleAndFutureDatesAreRejected()throws Exception {
        has(decode(bill().put("issueDate","2026-02-30").toString()),"INVALID_DATE");
        has(decode(bill().put("issueDate","2099-01-01").toString()),"DATE_RANGE");
    }
    @Test void totalAndLineArithmeticAreIndependentlyChecked()throws Exception {
        has(decode(bill().put("totalAmount","107.00").toString()),"TOTAL_MISMATCH");
        var bill=bill();((ObjectNode)bill.withArray("items").get(0)).put("quantity","3");
        has(decode(bill.toString()),"LINE_MISMATCH");
        bill=bill();((ObjectNode)bill.withArray("items").get(0)).put("amount","99.00");has(decode(bill.toString()),"LINES_SUM_MISMATCH");
    }
    @Test void negativeOverPreciseAndUnsupportedCurrencyRequireReview()throws Exception {
        has(decode(bill().put("taxAmount","-1.00").toString()),"INVALID_MONEY");
        has(decode(bill().put("taxAmount","6.001").toString()),"INVALID_MONEY");
        has(decode(bill().put("currency","USD").toString()),"UNSUPPORTED_CURRENCY");
    }
    @Test void fabricatedOrWrongFieldQuotesAreRejected()throws Exception {
        var bill=bill();((ObjectNode)bill.path("evidence").path("seller")).put("quote","这句话不在原文");has(decode(bill.toString()),"UNSUPPORTED_EVIDENCE");
        bill=bill();((ObjectNode)bill.path("evidence").path("seller")).put("quote","示例公司");has(decode(bill.toString()),"UNSUPPORTED_EVIDENCE");
        bill=bill();((ObjectNode)bill.path("items").get(0)).put("quantity","1").put("unitPrice","100.00");has(decode(bill.toString()),"UNSUPPORTED_EVIDENCE");
    }
    @Test void missingEvidenceAndMultiPageClaimsAreRejected()throws Exception {
        var bill=bill();bill.remove("evidence");has(decode(bill.toString()),"MISSING_EVIDENCE");
        bill=bill();((ObjectNode)bill.path("evidence").path("buyer")).put("page",2);has(decode(bill.toString()),"MISSING_EVIDENCE");
    }
    @Test void textAndImageInputLimitsAreEnforced()throws Exception {
        assertThrows(IllegalArgumentException.class,()->DocumentInput.text(" "));
        assertThrows(IllegalArgumentException.class,()->DocumentInput.text("x".repeat(100001)));
        Path fake=temp.resolve("fake.png");Files.writeString(fake,"not a png");assertThrows(IllegalArgumentException.class,()->DocumentInput.loadImage(fake));
        Path huge=temp.resolve("huge.jpg");Files.write(huge,new byte[DocumentInput.MAX_IMAGE_BYTES+1]);assertThrows(IllegalArgumentException.class,()->DocumentInput.loadImage(huge));
        Path tiny=temp.resolve("tiny.png");ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB),"png",tiny.toFile());assertThrows(IllegalArgumentException.class,()->DocumentInput.loadImage(tiny));
    }
    DocumentInput image()throws Exception {
        Path png=temp.resolve("synthetic.png");ImageIO.write(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB),"png",png.toFile());return DocumentInput.loadImage(png);
    }
    @Test void imagesCannotBeSentByDefaultOrThroughDefaultDeepSeek()throws Exception {
        try(var fixture=new LocalInvoiceFixture()) {
            DocumentInput image=image();has(new ExtractionClient(fixture.settings(),CLOCK).extract(image),"VISION_NOT_ENABLED");
            var deepseek=new ModelSettings("deepseek",fixture.baseUrl(),"model","key",true,true,"vision-declared");
            has(new ExtractionClient(deepseek,CLOCK).extract(image),"VISION_NOT_ENABLED");assertEquals(0,fixture.calls.get());
        }
    }
    @Test void explicitImageRequestUsesDataUriAndStillNeedsHumanReview()throws Exception {
        try(var fixture=new LocalInvoiceFixture()) {
            var config=new ModelSettings("openai",fixture.baseUrl(),"text-model","key",true,true,"explicit-vision-model");
            ReviewResult result=new ExtractionClient(config,CLOCK).extract(image());
            var request=ExtractionClient.JSON.readTree(fixture.lastRequest);
            assertEquals("explicit-vision-model",request.path("model").asText());
            assertTrue(request.path("messages").get(1).path("content").get(1).path("image_url").path("url").asText().startsWith("data:image/png;base64,"));
            has(result,"IMAGE_CONTENT_UNVERIFIED");has(result,"INVALID_BBOX");
        }
    }
    @Test void bboxGeometryIsCheckedEvenWhenImageContentCannotBeVerified()throws Exception {
        var bill=bill();bill.path("evidence").forEach(node->{var b=((ObjectNode)node).putArray("bbox");b.add(0).add(0).add(101).add(50);});
        var result=new ExtractionClient(ModelSettings.fromEnvironment(Map.of()),CLOCK).decode(bill.toString(),image(),"fixture");has(result,"INVALID_BBOX");
        bill=bill();bill.path("evidence").forEach(node->{var b=((ObjectNode)node).putArray("bbox");b.add(0).add(0).add(50).add(50);});
        var line=(ObjectNode)bill.path("items").get(0).path("evidence");line.putArray("bbox").add(0).add(0).add(50).add(50);
        result=new ExtractionClient(ModelSettings.fromEnvironment(Map.of()),CLOCK).decode(bill.toString(),image(),"fixture");
        has(result,"IMAGE_CONTENT_UNVERIFIED");assertFalse(result.issues().stream().anyMatch(i->i.code().equals("INVALID_BBOX")));
    }
    @Test void httpFailureTruncationAndOversizeNeverBecomeSuccess()throws Exception {
        try(var fixture=new LocalInvoiceFixture()) {
            var client=new ExtractionClient(fixture.settings(),CLOCK);fixture.status=429;has(client.extract(text),"HTTP_STATUS");
            fixture.status=200;fixture.response=LocalInvoiceFixture.envelope(LocalInvoiceFixture.candidate(),"length");has(client.extract(text),"INCOMPLETE_GENERATION");
            fixture.response="x".repeat(1_000_001);has(client.extract(text),"MODEL_IO_OR_JSON");
        }
    }
    @Test void timeoutIsBoundedAndNotRetried()throws Exception {
        try(var fixture=new LocalInvoiceFixture()) {
            fixture.delayMillis=200;var result=new ExtractionClient(fixture.settings(),CLOCK,Duration.ofMillis(30)).extract(text);
            has(result,"MODEL_IO_OR_JSON");assertTrue(fixture.calls.get()<=1);
        }
    }
}
