package org.stt.reporting;

import java.util.List;

import org.stt.model.TimeTrackingItem;

public interface ItemGrouper {
	List<String> getGroupsOf(TimeTrackingItem item);
}
