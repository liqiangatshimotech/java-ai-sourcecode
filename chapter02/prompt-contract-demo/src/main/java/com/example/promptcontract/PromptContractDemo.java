package com.example.promptcontract;

public class PromptContractDemo {

    public static void main(String[] args) {
        String ticketText = args.length == 0
            ? "我今天登录不上后台，重置密码也收不到邮件，客户还在等我处理订单。"
            : String.join(" ", args);

        System.out.println("=== Prompt 契约模板 ===");
        System.out.println(PromptTemplates.promptContract());
        System.out.println("=== 工单分类 Prompt ===");
        System.out.println(PromptTemplates.ticketClassifierPrompt(ticketText));
    }
}
