package org.stt.analysis;

public interface ItemCategorizer {

	enum ItemCategory {

		BREAK, WORKTIME
	}

	ItemCategory getCategory(String comment);
}
