package com.example.chapter09.signal;

import java.time.LocalDate;
import java.util.List;

public final class ClaudeCodeSignalAnalysisDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        System.out.println("Provider: " + settings.summary());
        System.out.println();

        new SourceMapPackageInspector().inspectIfPresent()
                .ifPresentOrElse(
                        finding -> System.out.println("Local package: " + finding.summary()),
                        () -> System.out.println("Local package: claude-code-sourcemap not found, skip source-map check")
                );
        System.out.println();

        PromptMarkerEncoder encoder = new PromptMarkerEncoder();
        PromptMarkerInspector inspector = new PromptMarkerInspector();
        LocalDate reportedDate = LocalDate.of(2026, 6, 30);

        List<DetectionInput> cases = List.of(
                DetectionInput.withoutProxy("America/Los_Angeles", reportedDate),
                DetectionInput.withProxy("Asia/Shanghai", "https://proxy.example.cn/v1", reportedDate),
                DetectionInput.withProxy("Asia/Urumqi", "https://deepseek-relay.example.com/v1", reportedDate),
                DetectionInput.withProxy("Asia/Shanghai", "https://deepseek-gateway.example.cn/v1", reportedDate)
        );

        for (DetectionInput input : cases) {
            PromptMarkerEncoder.EncodedPromptLine encoded = encoder.encode(input);
            PromptMarker inspected = inspector.inspect(encoded.line());

            System.out.println("Input: zone=" + input.systemZone()
                    + ", proxy=" + input.proxyUrl().orElse("(none)"));
            System.out.println("Prompt line: " + encoded.line());
            System.out.println("Encoded marker: " + encoded.marker().explain());
            System.out.println("Inspected marker: " + inspected.explain());
            System.out.println("Unicode dump: " + UnicodePrinter.dump(encoded.line()));
            System.out.println();
        }
    }
}
