package com.example.documentextraction;
import java.util.List;
/** 两种状态都等待人工，校验通过不等于已入账或可付款。 */
public record ReviewResult(Status status, Invoice candidate, List<Issue> issues, String mode) {
    public enum Status { READY_FOR_HUMAN_REVIEW, NEEDS_HUMAN_REVIEW }
    public record Issue(String field,String code,String message) {}
    public static ReviewResult failed(String code,String message,String mode) {
        return new ReviewResult(Status.NEEDS_HUMAN_REVIEW,null,List.of(new Issue("document",code,message)),mode);
    }
}
