package org.stt.text;

import org.stt.config.WorktimeConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class WorktimeCategorizer implements ItemCategorizer {
    private final Map<String, ItemCategory> activityCategory;

	@Inject
    public WorktimeCategorizer(WorktimeConfig worktimeConfig) {
        activityCategory = worktimeConfig.getBreakActivities()
                .stream()
                .collect(Collectors.toMap(Function.identity(), a -> ItemCategory.BREAK));
    }

	@Override
	public ItemCategory getCategory(String comment) {
        return activityCategory.getOrDefault(comment, ItemCategory.WORKTIME);
    }
}
