package org.stt.analysis;

import java.util.Collection;
import org.stt.Configuration;
import org.stt.analysis.ItemCategorizer;

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
