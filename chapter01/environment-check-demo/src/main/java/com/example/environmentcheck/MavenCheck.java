package com.example.environmentcheck;

import java.util.concurrent.TimeUnit;

/** 检查 Maven 命令是否能启动并正常退出，不执行构建，不收集或回显其环境输出。 */
@FunctionalInterface
public interface MavenCheck {
    boolean available();

    static MavenCheck system() {
        return () -> {
            Process process = null;
            try {
                boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
                // Windows 的 Maven 入口是批处理文件；命令固定，不拼接用户提供的 shell 文本。
                java.util.List<String> command = windows
                    ? java.util.List.of("cmd.exe", "/d", "/c", "mvn.cmd", "--version")
                    : java.util.List.of("mvn", "--version");
                process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
                return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            } catch (java.io.IOException unavailable) { return false; }
            finally { if (process != null && process.isAlive()) process.destroyForcibly(); }
        };
    }
}
