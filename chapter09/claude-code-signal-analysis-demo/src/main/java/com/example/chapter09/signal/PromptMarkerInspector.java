package com.example.chapter09.signal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PromptMarkerInspector {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})([-/])(\\d{2})\\2(\\d{2})");

    public PromptMarker inspect(String line) {
        int apostrophe = findApostropheAfterToday(line);
        String separator = findDateSeparator(line);

        // 反解时只能知道“这行文本携带了什么 marker”，不能证明它为什么被生成。
        // 所以这里把三个布尔值解释为 marker 含义，而不是断言真实用户身份。
        boolean chinaTimezone = "/".equals(separator);
        boolean chinaLikeProxyHost = apostrophe == 0x2019 || apostrophe == 0x02B9;
        boolean aiLabLikeProxyHost = apostrophe == 0x02BC || apostrophe == 0x02B9;
        return new PromptMarker(chinaTimezone, chinaLikeProxyHost, aiLabLikeProxyHost, apostrophe, separator);
    }

    private int findApostropheAfterToday(String line) {
        String prefix = "Today";
        if (!line.startsWith(prefix)) {
            throw new IllegalArgumentException("line must start with Today");
        }
        int index = prefix.length();
        return line.codePointAt(index);
    }

    private String findDateSeparator(String line) {
        Matcher matcher = DATE_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new IllegalArgumentException("date must use yyyy-MM-dd or yyyy/MM/dd");
        }
        return matcher.group(2);
    }
}
