package com.example.chapter09.dsh.seam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地文件系统提供方：对应 dsh 的 fs-local。
 *
 * 特点是对路径不做任何策略限制，workspace 根目录只是默认落脚点。
 * “能不能写这里”这类问题在 dsh 里不属于文件系统本身，属于策略层
 * （沙箱提供方、审批事件、单调守卫），这是它和普通文件工具的分界。
 */
public final class LocalFileSystemProvider implements FileSystemSeam {

    @Override
    public String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Override
    public void write(Path path, String content) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    @Override
    public String providerName() {
        return "fs-local";
    }
}
