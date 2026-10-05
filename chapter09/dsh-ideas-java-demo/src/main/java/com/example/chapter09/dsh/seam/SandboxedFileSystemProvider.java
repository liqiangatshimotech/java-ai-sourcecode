package com.example.chapter09.dsh.seam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 沙箱文件系统提供方：对应 dsh 的 fs-sandbox / fs-e2b 这类受限实现。
 *
 * 它把所有路径规范化后限制在 root 内，越界直接抛 SecurityException。
 * 真实 dsh 里 e2b 版本还会让 fs-e2b 与 subprocess-e2b 共享同一个远程
 * Linux 运行时——文件和进程看到的是同一个沙箱世界。
 *
 * 注意这里体现的分工：消费者（write_file 工具）不知道沙箱存在；
 * 策略失败会被工具流水线规范化成 isError 结果回给模型。
 */
public final class SandboxedFileSystemProvider implements FileSystemSeam {

    private final Path root;

    public SandboxedFileSystemProvider(Path root) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    @Override
    public String read(Path path) throws IOException {
        return Files.readString(confine(path), StandardCharsets.UTF_8);
    }

    @Override
    public void write(Path path, String content) throws IOException {
        Path confined = confine(path);
        Path parent = confined.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(confined, content, StandardCharsets.UTF_8);
    }

    /** 规范化并校验：相对 root 解析，../ 逃逸直接拒绝。 */
    private Path confine(Path path) {
        Path resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("sandbox denied: " + path + " escapes " + root);
        }
        return resolved;
    }

    @Override
    public String providerName() {
        return "fs-sandbox(" + root.getFileName() + ")";
    }
}
