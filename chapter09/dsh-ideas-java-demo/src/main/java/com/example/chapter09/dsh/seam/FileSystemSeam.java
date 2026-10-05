package com.example.chapter09.dsh.seam;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 文件系统接缝：对应 dsh 的 fs 包（ctx.fs）。
 *
 * dsh 的 Bash、文件编辑工具不直接碰 java.nio，而是消费这个接缝。
 * 实现提供方可以是 fs-local（本地盘）、fs-sandbox（受限目录）、
 * fs-e2b（远程沙箱盘）。换提供方时，工具层代码一行不改。
 */
public interface FileSystemSeam {

    String read(Path path) throws IOException;

    void write(Path path, String content) throws IOException;

    /** 提供方名字：local / sandbox / e2b...，日志里用来区分现在谁在服务。 */
    String providerName();
}
