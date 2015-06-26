package org.stt.text;

import java.util.List;

public interface ExpansionProvider {
	List<String> getPossibleExpansions(String text);
}
