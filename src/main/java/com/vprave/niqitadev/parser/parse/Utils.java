package com.vprave.niqitadev.parser.parse;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public final class Utils {
    public static String removeAll(Pattern pattern, CharSequence sequence) {
        return pattern.matcher(sequence).replaceAll(StringUtils.EMPTY);
    }
}
