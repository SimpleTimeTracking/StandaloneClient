package org.stt.text;

import dagger.Module;
import dagger.Provides;
import org.stt.config.YamlConfigService;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Module
public class TextModule {
    private static final Logger LOG = Logger.getLogger(TextModule.class.getName());

    private TextModule() {
    }

    @Provides
    static Collection<ExpansionProvider> provideExpansionProvider(CommonPrefixGrouper commonPrefixGrouper,
                                                                  JiraExpansionProvider jiraExpansionProvider) {
        return Arrays.asList(commonPrefixGrouper, jiraExpansionProvider);
    }

    @Provides
    static ItemCategorizer provideItemCategorizer(WorktimeCategorizer worktimeCategorizer) {
        return worktimeCategorizer;
    }

    @Provides
    static ItemGrouper provideItemGrouper(TimeTrackingItemQueries queries,
                                          YamlConfigService yamlConfig,
                                          CommonPrefixGrouper commonPrefixGrouper) {
        try (Stream<TimeTrackingItem> items = queries.queryAllItems()) {
            commonPrefixGrouper.scanForGroups(items);
        }
        for (String item : yamlConfig.getConfig().getPrefixGrouper().getBaseLine()) {
            LOG.info(() -> String.format("Adding baseline item '%s'", item));
            commonPrefixGrouper.learnLine(item);
        }

        return commonPrefixGrouper;
    }
}
