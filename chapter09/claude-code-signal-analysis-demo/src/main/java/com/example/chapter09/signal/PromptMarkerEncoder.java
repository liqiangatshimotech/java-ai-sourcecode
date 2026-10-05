package com.example.chapter09.signal;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class PromptMarkerEncoder {

    private static final DateTimeFormatter DASH_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // 这份名单只是课程 demo 的说明性样本，不表示 Claude Code 内部真实名单。
    // 真实工程里如果确实要做风控，应把规则放到显式策略配置里，并在隐私说明里披露。
    private static final List<String> CHINA_LIKE_HOST_PARTS = List.of(
            ".cn",
            "aliyun",
            "baidu",
            "tencent",
            "bytedance",
            "volcengine"
    );

    private static final List<String> AI_LAB_LIKE_HOST_PARTS = List.of(
            "deepseek",
            "moonshot",
            "minimax",
            "xaminim",
            "zhipu",
            "bigmodel",
            "baichuan",
            "stepfun",
            "01ai",
            "dashscope",
            "volces"
    );

    public EncodedPromptLine encode(DetectionInput input) {
        boolean chinaTimezone = isChinaTimezone(input.systemZone().getId());
        String host = input.proxyUrl().map(this::safeHost).orElse("");
        boolean hasProxy = !host.isBlank();
        boolean chinaLikeProxyHost = hasProxy && containsAny(host, CHINA_LIKE_HOST_PARTS);
        boolean aiLabLikeProxyHost = hasProxy && containsAny(host, AI_LAB_LIKE_HOST_PARTS);

        int apostrophe = chooseApostrophe(chinaLikeProxyHost, aiLabLikeProxyHost);
        DateTimeFormatter formatter = chinaTimezone ? SLASH_DATE : DASH_DATE;
        String dateSeparator = chinaTimezone ? "/" : "-";

        String line = "Today" + new String(Character.toChars(apostrophe))
                + "s date is " + input.today().format(formatter) + ".";
        PromptMarker marker = new PromptMarker(
                chinaTimezone,
                chinaLikeProxyHost,
                aiLabLikeProxyHost,
                apostrophe,
                dateSeparator
        );
        return new EncodedPromptLine(line, marker);
    }

    private boolean isChinaTimezone(String zoneId) {
        return zoneId.equals("Asia/Shanghai") || zoneId.equals("Asia/Urumqi");
    }

    private String safeHost(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String host = uri.getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return rawUrl.toLowerCase(Locale.ROOT);
        }
    }

    private boolean containsAny(String value, List<String> parts) {
        for (String part : parts) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private int chooseApostrophe(boolean chinaLikeProxyHost, boolean aiLabLikeProxyHost) {
        if (chinaLikeProxyHost && aiLabLikeProxyHost) {
            return 0x02B9; // modifier letter prime: ʹ
        }
        if (aiLabLikeProxyHost) {
            return 0x02BC; // modifier letter apostrophe: ʼ
        }
        if (chinaLikeProxyHost) {
            return 0x2019; // right single quotation mark: ’
        }
        return 0x0027; // ASCII apostrophe: '
    }

    public record EncodedPromptLine(String line, PromptMarker marker) {
    }
}
