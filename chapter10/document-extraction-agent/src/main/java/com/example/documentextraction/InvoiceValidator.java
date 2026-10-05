package com.example.documentextraction;
import java.math.*;
import java.time.*;
import java.util.*;
import com.example.documentextraction.ReviewResult.*;

/** 模型只提取，Java独立核对金额、日期与证据，不把模型自己的“正确”声明当依据。 */
public final class InvoiceValidator {
    private final Clock clock;
    public InvoiceValidator(Clock clock){this.clock=clock;}
    public ReviewResult validate(Invoice bill,DocumentInput input,String mode) {
        List<Issue> issues=new ArrayList<>();
        if(bill==null)return ReviewResult.failed("MISSING_DOCUMENT","没有候选票据",mode);
        Map<String,String> required=new LinkedHashMap<>();
        required.put("invoiceNumber",bill.invoiceNumber());required.put("issueDate",bill.issueDate());
        required.put("seller",bill.seller());required.put("buyer",bill.buyer());required.put("currency",bill.currency());
        required.forEach((field,value)->{if(value==null||value.isBlank())add(issues,field,"MISSING_FIELD","字段缺失");});
        if(bill.currency()!=null && !bill.currency().equals("CNY"))add(issues,"currency","UNSUPPORTED_CURRENCY","当前业务仅支持CNY，其他币种交人工");
        try {
            LocalDate date=LocalDate.parse(bill.issueDate());
            if(date.isAfter(LocalDate.now(clock))||date.isBefore(LocalDate.of(2000,1,1)))add(issues,"issueDate","DATE_RANGE","日期不在2000年至今天的范围内");
        }catch(Exception e){add(issues,"issueDate","INVALID_DATE","需要真实存在的ISO日期yyyy-MM-dd");}
        boolean net=money(bill.netAmount(),"netAmount",issues),tax=money(bill.taxAmount(),"taxAmount",issues),total=money(bill.totalAmount(),"totalAmount",issues);
        if(net&&tax&&total&&bill.netAmount().add(bill.taxAmount()).compareTo(bill.totalAmount())!=0)
            add(issues,"totalAmount","TOTAL_MISMATCH","未税金额加税额不等于价税合计");
        if(net)required.put("netAmount",bill.netAmount().setScale(2).toPlainString());
        if(tax)required.put("taxAmount",bill.taxAmount().setScale(2).toPlainString());
        if(total)required.put("totalAmount",bill.totalAmount().setScale(2).toPlainString());
        required.forEach((field,value)->checkEvidence(field,value,bill.evidence()==null?null:bill.evidence().get(field),input,issues));
        if(bill.items()==null||bill.items().isEmpty()||bill.items().size()>100) {
            add(issues,"items","INVALID_ITEMS","明细行数应为1至100");
        } else {
            BigDecimal sum=BigDecimal.ZERO;boolean linesValid=true;
            for(int i=0;i<bill.items().size();i++) {
                var line=bill.items().get(i);String field="items["+i+"]";
                if(line==null){add(issues,field,"MISSING_LINE","明细不能为空");linesValid=false;continue;}
                if(line.description()==null||line.description().isBlank())add(issues,field,"MISSING_DESCRIPTION","明细名称缺失");
                boolean price=money(line.unitPrice(),field+".unitPrice",issues),amount=money(line.amount(),field+".amount",issues);
                boolean qty=line.quantity()!=null&&line.quantity().signum()>0&&line.quantity().scale()<=3&&line.quantity().compareTo(new BigDecimal("100000"))<=0;
                if(!qty)add(issues,field+".quantity","INVALID_QUANTITY","数量须大于0、不超过100000且最多3位小数");
                if(qty&&price&&amount&&line.quantity().multiply(line.unitPrice()).setScale(2,RoundingMode.HALF_UP).compareTo(line.amount())!=0)
                    add(issues,field,"LINE_MISMATCH","数量乘单价按分四舍五入后与明细金额不符");
                if(amount)sum=sum.add(line.amount());else linesValid=false;
                checkEvidence(field+".description",line.description(),line.evidence(),input,issues);
                if(qty)checkEvidence(field+".quantity",line.quantity().stripTrailingZeros().toPlainString(),line.evidence(),input,issues);
                if(price)checkEvidence(field+".unitPrice",line.unitPrice().setScale(2).toPlainString(),line.evidence(),input,issues);
                if(amount)checkEvidence(field+".amount",line.amount().setScale(2).toPlainString(),line.evidence(),input,issues);
            }
            if(net&&linesValid&&sum.compareTo(bill.netAmount())!=0)add(issues,"netAmount","LINES_SUM_MISMATCH","明细金额之和不等于未税金额");
        }
        // 图片bbox只能证明坐标合法，无法证明字段真的出现在该区域，必须看图复核。
        if(input.image())add(issues,"evidence","IMAGE_CONTENT_UNVERIFIED","仅验证图片证据坐标边界，字段与图中文字须人工核对");
        return new ReviewResult(issues.isEmpty()?Status.READY_FOR_HUMAN_REVIEW:Status.NEEDS_HUMAN_REVIEW,bill,List.copyOf(issues),mode);
    }
    private boolean money(BigDecimal amount,String field,List<Issue> issues) {
        if(amount==null||amount.signum()<0||amount.scale()>2||amount.compareTo(new BigDecimal("1000000000"))>0) {
            add(issues,field,"INVALID_MONEY","金额缺失、负数、超过10亿元或小数多于2位");return false;
        }return true;
    }
    private void checkEvidence(String field,String value,Invoice.Evidence proof,DocumentInput input,List<Issue> issues) {
        if(proof==null||!Integer.valueOf(1).equals(proof.page())) {add(issues,field,"MISSING_EVIDENCE","单页票据需要page=1的证据");return;}
        if(input.image()) {
            List<Integer>b=proof.bbox();
            if(b==null||b.size()!=4||b.stream().anyMatch(Objects::isNull)||b.get(0)<0||b.get(1)<0||b.get(2)<=b.get(0)||b.get(3)<=b.get(1)||b.get(2)>input.width()||b.get(3)>input.height())
                add(issues,field,"INVALID_BBOX","bbox必须位于原图像素范围内且面积为正");
        }else {
            if(proof.quote()==null||proof.quote().isBlank()||!input.text().contains(proof.quote())||value==null||!containsValue(proof.quote(),value))
                add(issues,field,"UNSUPPORTED_EVIDENCE","quote必须逐字出现在输入文本中，并包含抽取值");
        }
    }
    private static boolean containsValue(String quote,String value) {
        // 数字要匹配完整词元；数量1不能因为引用里有100.00就算得到证据。
        if(value.matches("[0-9]+(\\.[0-9]+)?")) {
            return java.util.regex.Pattern.compile("(?<![0-9.])"+java.util.regex.Pattern.quote(value)+"(?![0-9.])").matcher(quote).find();
        }
        return quote.contains(value);
    }
    private static void add(List<Issue> issues,String field,String code,String message){issues.add(new Issue(field,code,message));}
}
