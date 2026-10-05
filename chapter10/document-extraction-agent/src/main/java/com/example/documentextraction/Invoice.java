package com.example.documentextraction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 模型返回候选数据；金额使用十进制，禁止用double处理票据金额。 */
public record Invoice(String invoiceNumber, String issueDate, String seller, String buyer,
                      String currency, BigDecimal netAmount, BigDecimal taxAmount, BigDecimal totalAmount,
                      List<LineItem> items, Map<String,Evidence> evidence) {
    /** qty×unitPrice按分四舍五入后须等于amount，各行amount之和须等于netAmount。 */
    public record LineItem(String description, BigDecimal quantity, BigDecimal unitPrice,
                           BigDecimal amount, Evidence evidence) {}
    /** 本例只接收单页：page=1；bbox为原图像素坐标[x1,y1,x2,y2]；quote是原文摘录。 */
    public record Evidence(Integer page, List<Integer> bbox, String quote) {}
}
