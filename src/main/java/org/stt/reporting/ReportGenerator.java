package org.stt.reporting;

import java.util.List;

import org.stt.model.ReportingItem;

/**
 * All classes providing reporting functionality should implement this
 */
public interface ReportGenerator {

	/**
	 * @return all report items.
	 */
	List<ReportingItem> report();
}
