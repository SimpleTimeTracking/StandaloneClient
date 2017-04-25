package org.stt.update;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

public class VersionComparator implements Comparator<String>, Serializable {
    @Override
    public int compare(String a, String b) {
        int[] aVersionParts = Arrays.stream(a.split("\\.")).mapToInt(this::asVersionPart).toArray();
        int[] bVersionParts = Arrays.stream(b.split("\\.")).mapToInt(this::asVersionPart).toArray();
        for (int i = 0; i < Math.min(aVersionParts.length, bVersionParts.length); i++) {
            if (aVersionParts[i] < bVersionParts[i]) {
                return -1;
            } else if (aVersionParts[i] > bVersionParts[i]) {
                return 1;
            }
        }
        return Integer.compare(aVersionParts.length, bVersionParts.length);
    }

    private Integer asVersionPart(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
