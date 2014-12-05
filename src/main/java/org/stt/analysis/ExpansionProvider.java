package org.stt.analysis;

import java.util.List;

public interface ExpansionProvider {
	List<String> getPossibleExpansions(String text);
}
