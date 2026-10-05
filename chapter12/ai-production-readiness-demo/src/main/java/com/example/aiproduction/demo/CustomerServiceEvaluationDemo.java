package com.example.aiproduction.demo;

import com.example.aiproduction.eval.CustomerServiceReleaseGate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.List;

/** 可在 CI 执行的发布检查；退出异常会让 Maven / CI 失败，不能只打印报告后继续上线。 */
public class CustomerServiceEvaluationDemo {
    public static void main(String[] args) throws Exception {
        String base=System.getenv().getOrDefault("CUSTOMER_SERVICE_BASE_URL","http://127.0.0.1:8080");
        String token=System.getenv().getOrDefault("SUPPORT_ACCESS_TOKEN","");
        String dataset=System.getenv("EVAL_DATASET");
        List<CustomerServiceReleaseGate.Case> cases;
        ObjectMapper json=new ObjectMapper();
        if(dataset==null) {
            try(var input=CustomerServiceEvaluationDemo.class.getResourceAsStream("/evaluation/customer-service.json")) {
                cases=json.readValue(input,new TypeReference<>(){});
            }
        } else cases=json.readValue(Files.readString(Path.of(dataset)),new TypeReference<>(){});
        Path output=Path.of(System.getenv().getOrDefault("EVAL_OUTPUT","output/eval-"+System.currentTimeMillis()+".ndjson"));
        var report=new CustomerServiceReleaseGate(base,token).run(cases,output,
            Double.parseDouble(System.getenv().getOrDefault("EVAL_MIN_PASS_RATE","1.0")),
            Long.parseLong(System.getenv().getOrDefault("EVAL_MAX_P95_MS","35000")));
        System.out.printf("用例=%d 通过率=%.2f p95=%dms 允许发布=%s 报告=%s%n",
            report.results().size(),report.passRate(),report.p95Ms(),report.releaseAllowed(),output);
        if(!report.releaseAllowed()) throw new IllegalStateException("发布门禁未通过，请检查持久化报告");
    }
}
