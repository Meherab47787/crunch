package com.crustcrunch.service;

import java.text.Normalizer;
import java.util.Locale;

final class SlugUtil {

    private SlugUtil() {
    }

    static String slugify(String input) {
        String s = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase(Locale.ROOT).replace("&", " and ").replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "item" : s;
    }
}
