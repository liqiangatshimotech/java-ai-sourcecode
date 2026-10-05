package com.example.documentextraction;
import java.nio.file.Path;
import java.time.*;

public final class DocumentExtractionDemo {
    public static void main(String[] args)throws Exception {
        if(args.length==0 || (args.length==1&&args[0].equals("--fixture"))) {
            // 默认只向本机临时HTTP服务发送合成文本，不外传任何本地文件。
            try(var fixture=new LocalInvoiceFixture()) {
                var result=new ExtractionClient(fixture.settings(),Clock.fixed(Instant.parse("2026-09-09T00:00:00Z"),ZoneOffset.UTC))
                        .extract(DocumentInput.text(LocalInvoiceFixture.TEXT));
                result=new ReviewResult(result.status(),result.candidate(),result.issues(),"offline-http-fixture（合成数据，非OCR识别）");
                System.out.println(ExtractionClient.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result));
            }
            return;
        }
        ReviewResult result;
        try {
            if(args.length!=2 || !(args[0].equals("--text")||args[0].equals("--image")))
                throw new IllegalArgumentException("用法：--fixture 或 --text 文件路径 或 --image PNG/JPEG路径");
            DocumentInput input=args[0].equals("--text")?DocumentInput.loadText(Path.of(args[1])):DocumentInput.loadImage(Path.of(args[1]));
            result=new ExtractionClient(ModelSettings.fromEnvironment(System.getenv()),Clock.systemDefaultZone()).extract(input);
        }catch(Exception e){result=ReviewResult.failed("INPUT_OR_CONFIGURATION","输入文件或配置无效："+e.getClass().getSimpleName(),"local-validation");}
        System.out.println(ExtractionClient.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }
}
