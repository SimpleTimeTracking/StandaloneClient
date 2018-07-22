package org.stt.text

import org.stt.config.WorktimeConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorktimeCategorizer @Inject
constructor(worktimeConfig: WorktimeConfig) : ItemCategorizer {
    private val activityCategory: Map<String, ItemCategorizer.ItemCategory> = worktimeConfig.breakActivities
            .map { it to ItemCategorizer.ItemCategory.BREAK }
            .toMap()

    override fun getCategory(comment: String): ItemCategorizer.ItemCategory {
        return activityCategory.getOrDefault(comment, ItemCategorizer.ItemCategory.WORKTIME)
    }
}
