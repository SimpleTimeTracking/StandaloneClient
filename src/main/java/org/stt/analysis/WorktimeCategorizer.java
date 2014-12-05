package org.stt.analysis;

import org.stt.Configuration;

import java.util.Collection;

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
