package com.example.chapter09.dsh.core;

import com.example.chapter09.dsh.llm.LlmSeam;
import com.example.chapter09.dsh.plugin.Plugin;
import com.example.chapter09.dsh.plugin.PluginContext;
import com.example.chapter09.dsh.seam.ApprovalSeam;
import com.example.chapter09.dsh.seam.FileSystemSeam;
import com.example.chapter09.dsh.seam.LocalFileSystemProvider;
import com.example.chapter09.dsh.seam.SandboxedFileSystemProvider;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 能力接缝的 Provider 插件集合。
 *
 * 每个类都是一个独立的“实现提供方”，都只做一件事：
 * 往 ctx 上提供一个 seam 实现。替换实现 = 卸载旧插件、挂载新插件，
 * 消费方（工具、agent loop）完全无感。
 */
public final class SeamProviders {

    private SeamProviders() {
    }

    /** llm 提供方：把任意 LlmSeam 实现挂到 ctx.llm 上。 */
    public static Plugin llm(LlmSeam adapter) {
        return ctx -> ctx.provideService("llm", adapter);
    }

    /** fs-local 提供方。 */
    public static Plugin localFs() {
        return ctx -> ctx.provideService("fs", new LocalFileSystemProvider());
    }

    /** fs-sandbox 提供方：限制在 root 内。 */
    public static Plugin sandboxedFs(Path root) {
        return ctx -> {
            try {
                ctx.provideService("fs", new SandboxedFileSystemProvider(root));
            } catch (IOException e) {
                throw new IllegalStateException("cannot create sandbox root", e);
            }
        };
    }

    /** 审批提供方：控制台版 ctx.approval。 */
    public static Plugin approval(ApprovalSeam approval) {
        return ctx -> ctx.provideService("approval", approval);
    }
}
