package org.stt.searching;

import java.util.List;

public interface ExpansionProvider {
	List<String> getPossibleExpansions(String text);
}
