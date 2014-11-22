package org.stt.reporting;

import java.util.Collection;
import org.stt.Configuration;

public class WorktimeCategorizer implements ItemCategorizer {

	private Configuration configuration;

	public WorktimeCategorizer(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public ItemCategory getCategory(String comment) {
		Collection<String> breakTimeComments = configuration
				.getBreakTimeComments();
		if (breakTimeComments.contains(comment)) {
			return ItemCategory.BREAK;
		}
		return ItemCategory.WORKTIME;
	}

}
