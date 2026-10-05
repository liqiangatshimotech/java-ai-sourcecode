package com.example.chapter09.mimocode.context;

import java.util.ArrayList;
import java.util.List;

/**
 * checkpoint 触发策略。这里按 MiMo-Code 当前源码里的默认密度实现：
 * 小窗口按 20% 间隔，扩展窗口按 10% 间隔，超大窗口按 5% 间隔。
 *
 * 注意：触发 checkpoint 不等于立刻把主流程停下来。真实实现会让
 * checkpoint-writer 作为后台子 Agent 写文件，主 Agent 继续处理当前任务。
 */
public final class CheckpointThresholdPolicy {
    private CheckpointThresholdPolicy() {
    }

    public static List<Integer> defaultThresholdsFor(int usableWindow) {
        if (usableWindow < 25_000) {
            return List.of();
        }
        if (usableWindow <= 200_000) {
            return percentages(usableWindow, 20, 40, 60, 80);
        }
        if (usableWindow <= 500_000) {
            return percentages(usableWindow, 10, 20, 30, 40, 50, 60, 70, 80, 90);
        }
        List<Integer> values = new ArrayList<>();
        for (int percent = 5; percent <= 90; percent += 5) {
            values.add(usableWindow * percent / 100);
        }
        return values;
    }

    private static List<Integer> percentages(int usableWindow, int... values) {
        List<Integer> thresholds = new ArrayList<>();
        for (int value : values) {
            thresholds.add(usableWindow * value / 100);
        }
        return thresholds;
    }
}
