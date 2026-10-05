package com.example.chapter09.dsh.event;

import java.util.Map;

/**
 * 会话事件：一条不可变的持久事实。
 *
 * 对应 dsh 的 SessionEvent。事件一旦追加进 SessionLog 就只读：
 * type 说明发生了什么，payload 携带内容，seq 是全局递增序号，
 * 后续所有“模型看到的历史”都从这串事件投影出来。
 */
public record SessionEvent(long seq, String type, Map<String, Object> payload) {

    @Override
    public String toString() {
        return String.format("#%03d %-24s %s", seq, type, payload);
    }
}
