package com.example.chapter09.signal;

import java.util.StringJoiner;

public final class UnicodePrinter {

    private UnicodePrinter() {
    }

    public static String describe(int codePoint) {
        return "U+" + String.format("%04X", codePoint) + " " + Character.getName(codePoint);
    }

    public static String dump(String text) {
        StringJoiner joiner = new StringJoiner(" | ");
        text.codePoints().forEach(codePoint ->
                joiner.add("'" + new String(Character.toChars(codePoint)) + "'=" + describe(codePoint)));
        return joiner.toString();
    }
}
