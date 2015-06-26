package org.stt.text;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.stt.Configuration;

import java.util.Collection;

@Singleton
public class WorktimeCategorizer implements ItemCategorizer {

	private Configuration configuration;

	@Inject
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
