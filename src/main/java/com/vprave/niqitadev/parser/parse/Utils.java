package com.vprave.niqitadev.parser.parse;

import java.util.regex.Pattern;

public final class Utils {
    public static String removeAll(Pattern pattern, CharSequence sequence) {
        return pattern.matcher(sequence).replaceAll("");
    }

}
