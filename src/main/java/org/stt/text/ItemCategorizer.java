package org.stt.text;

public interface ItemCategorizer {

	enum ItemCategory {

		BREAK, WORKTIME
	}

	ItemCategory getCategory(String comment);
}
