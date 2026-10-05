package com.example.documentextraction;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import javax.imageio.ImageIO;

/** 只接收UTF-8文本或单张JPEG/PNG，不自动下载URL，也不执行票据里的任何指令。 */
public record DocumentInput(String text, String dataUri, int width, int height) {
    public static final int MAX_IMAGE_BYTES=5*1024*1024;
    public boolean image(){return dataUri!=null;}
    public static DocumentInput text(String text) {
        if(text==null || text.isBlank() || text.length()>100_000) throw new IllegalArgumentException("OCR文本应为1到100000字符");
        return new DocumentInput(text,null,0,0);
    }
    public static DocumentInput loadText(Path file)throws Exception {
        if(Files.size(file)>256*1024) throw new IllegalArgumentException("文本文件超过256KB");
        return text(Files.readString(file,StandardCharsets.UTF_8));
    }
    public static DocumentInput loadImage(Path file)throws Exception {
        if(!Files.isRegularFile(file) || Files.size(file)>MAX_IMAGE_BYTES) throw new IllegalArgumentException("图片不是普通文件或超过5MB");
        String name=file.getFileName().toString().toLowerCase(Locale.ROOT);
        if(!(name.endsWith(".png")||name.endsWith(".jpg")||name.endsWith(".jpeg"))) throw new IllegalArgumentException("仅接受PNG/JPEG");
        byte[] bytes=Files.readAllBytes(file);
        if(bytes.length>MAX_IMAGE_BYTES) throw new IllegalArgumentException("图片超过5MB");
        try(var input=ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw new IllegalArgumentException("图片无法解码");
            var reader=readers.next();
            try {
                reader.setInput(input);String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if(!Set.of("png","jpeg","jpg").contains(format)) throw new IllegalArgumentException("图片内容格式不是PNG/JPEG");
                int w=reader.getWidth(0),h=reader.getHeight(0);
                // 先检查元信息再解码，避免小文件声明巨幅像素导致内存耗尽。
                if(w<32||h<32||w>8192||h>8192||(long)w*h>20_000_000) throw new IllegalArgumentException("图片边长须32至8192，总像素不超过2000万");
                reader.read(0); // 确认真正可解码，不能只凭扩展名和头部认为它是完整图片。
                String mime=format.equals("png")?"image/png":"image/jpeg";
                return new DocumentInput(null,"data:"+mime+";base64,"+Base64.getEncoder().encodeToString(bytes),w,h);
            }finally{reader.dispose();}
        }
    }
}
