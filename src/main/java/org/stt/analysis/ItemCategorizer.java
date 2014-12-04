package org.stt.analysis;

public interface ItemCategorizer {

	public enum ItemCategory {

		BREAK, WORKTIME
	}

	ItemCategory getCategory(String comment);
}
