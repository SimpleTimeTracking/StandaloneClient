package org.stt;

import java.util.Objects;

public class Strings {
    private Strings() {
    }

    public static String commonPrefix(String a, String b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        for (int i = 0; i < a.length() && i < b.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a;
    }
}
