package org.stt;

public class States {
    private States() {
    }

    public static void requireThat(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
