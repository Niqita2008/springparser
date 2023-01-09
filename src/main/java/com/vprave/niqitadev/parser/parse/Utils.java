package com.vprave.niqitadev.parser.parse;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Utils {
    public static final Pattern
            date = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}|\\d{1,2} +[а-я]+ +\\d{4}|\\d{2}-\\d{2}-\\d{4}", 0),
            uuid = Pattern.compile("[a-f\\-\\dA-F]{8}-([a-f\\-\\dA-F]{4}-){3}[a-f\\-\\dA-F]{12}(-[a-f\\-\\dA-F])?", 0);
    public static String removeAll(Pattern pattern, CharSequence sequence) {
        return pattern.matcher(sequence).replaceAll("");
    }

    public static String getColumn(String page, int start, int startLine, int count) {
        return page.lines().skip(startLine).limit(count).map(l -> l.substring(start)).collect(Collectors.joining());
    }

}
