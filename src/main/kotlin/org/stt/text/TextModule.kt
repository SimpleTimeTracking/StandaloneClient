package org.stt.text

import dagger.Module
import dagger.Provides
import java.util.*

@Module
class TextModule {

    @Provides
    fun provideItemCategorizer(worktimeCategorizer: WorktimeCategorizer): ItemCategorizer {
        return worktimeCategorizer
    }

    @Provides
    fun provideExpansionProvider(commonPrefixGrouper: CommonPrefixGrouper,
                                 jiraExpansionProvider: JiraExpansionProvider): Collection<ExpansionProvider> {
        return Arrays.asList(commonPrefixGrouper, jiraExpansionProvider)
    }

    @Provides
    fun provideItemGrouper(grouper: CommonPrefixGrouper): ItemGrouper {
        return grouper
    }
}
