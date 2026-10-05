package com.example.chapter09.dsh.core;

import com.example.chapter09.dsh.plugin.Disposer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统提示组装服务：对应 dsh 的 core/system-prompt 包（ctx.systemPrompt）。
 *
 * 每个插件可以贡献一段提示词片段（例如“遵守仓库代码规范”），
 * 片段的注册是可逆副作用：插件卸载，它对系统提示的影响随之消失。
 */
public final class SystemPromptService {

    private final Map<String, String> fragments = new LinkedHashMap<>();

    /** 注册一个片段，返回撤销动作。 */
    public Disposer addFragment(String name, String text) {
        fragments.put(name, text);
        return () -> fragments.remove(name);
    }

    public String assemble() {
        return String.join("\n", fragments.values());
    }
}
