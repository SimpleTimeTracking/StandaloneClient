package org.stt.text;

import dagger.Module;
import dagger.Provides;

import java.util.Arrays;
import java.util.Collection;

@Module
public class TextModule {
    private TextModule() {
    }

    @Provides
    static ItemCategorizer provideItemCategorizer(WorktimeCategorizer worktimeCategorizer) {
        return worktimeCategorizer;
    }

    @Provides
    static Collection<ExpansionProvider> provideExpansionProvider(CommonPrefixGrouper commonPrefixGrouper,
                                                                  JiraExpansionProvider jiraExpansionProvider) {
        return Arrays.asList(commonPrefixGrouper, jiraExpansionProvider);
    }

    @Provides
    static ItemGrouper provideItemGrouper(CommonPrefixGrouper grouper) {
        return grouper;
    }
}
