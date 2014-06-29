package org.stt.reporting;

public interface ItemCategorizer {

	public enum ItemCategory {

		BREAK, WORKTIME
	}

	ItemCategory getCategory(String comment);
}
