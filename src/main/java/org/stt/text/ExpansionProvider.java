package org.stt.text;

import java.util.List;

@FunctionalInterface
public interface ExpansionProvider {
    List<String> getPossibleExpansions(String text);
}
