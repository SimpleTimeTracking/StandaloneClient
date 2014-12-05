package org.stt.reporting;

import org.stt.analysis.ItemGrouper;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matches groups of quoted text. The quotes can be any non-alphabetic
 * characters.
 * 
 * @author bytekeeper
 * 
 */
public class QuotedItemGrouper implements ItemGrouper {

	@Override
	public List<String> getGroupsOf(String text) {
		checkNotNull(text);
		List<String> groups = new ArrayList<>();
		int index = 0;
		while (index < text.length()) {
			char delimiter = text.charAt(index);
			if (!Character.isWhitespace(delimiter)) {

				if (!Character.isAlphabetic(delimiter)) {
					int endOfQuote = text.indexOf(delimiter, index + 1);
					if (endOfQuote > 0) {
						groups.add(text.substring(index + 1, endOfQuote));
						index = endOfQuote;
					} else {
						groups.add(text.substring(index));
						index = text.length();
					}
				} else {
					groups.add(text.substring(index));
					index = text.length();
				}
			}
			index++;
		}
		return groups;
	}
}
