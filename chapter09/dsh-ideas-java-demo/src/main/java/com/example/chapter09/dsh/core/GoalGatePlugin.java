package com.example.chapter09.dsh.core;

import com.example.chapter09.dsh.event.SessionLog;

/**
 * Goal gate：挂在 agent/turn-stopping 上的 serial 终止检查点。
 *
 * 模型说“我做完了”不算数——裁判逻辑检查最后一个 assistant 消息，
 * 没有出现完成标记就投否决票，把轮次顶回执行循环。
 * 这是把“生成-验证差距”工程化的一种最小形态：
 * 停止条件是系统判定的，不是模型自己声称的。
 */
public final class GoalGatePlugin {

    public static void load(com.example.chapter09.dsh.plugin.PluginContext ctx) {
        ctx.load("goal-gate", c -> c.onSerial("agent/turn-stopping", args -> {
            SessionLog session = (SessionLog) args[0];
            String lastAssistant = session.events().reversed().stream()
                    .filter(e -> "assistant/message".equals(e.type()))
                    .map(e -> String.valueOf(e.payload().get("text")))
                    .findFirst()
                    .orElse("");
            if (lastAssistant.contains("DONE")) {
                return null; // 弃权：允许停止
            }
            System.out.println("    [goal-gate] 最终回答没有 DONE 标记，否决停止，继续执行");
            return Boolean.TRUE; // 否决：turn 继续
        }));
    }
}
