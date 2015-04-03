package org.stt.analysis;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.stt.YamlConfigService;
import org.stt.persistence.ItemReader;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by dante on 04.12.14.
 */
public class AnalysisModule extends AbstractModule {
    private static final Logger LOG = Logger.getLogger(AnalysisModule.class.getName());

    @Override
    protected void configure() {
        bind(ItemGrouper.class).to(CommonPrefixGrouper.class);
        bind(ExpansionProvider.class).to(CommonPrefixGrouper.class);
        bind(ItemCategorizer.class).to(WorktimeCategorizer.class);
    }

    @Provides
    CommonPrefixGrouper provideCommonPrefixGrouper(ItemReader reader, YamlConfigService yamlConfig) {
        CommonPrefixGrouper commonPrefixGrouper = new CommonPrefixGrouper();
        try (ItemReader itemReader=reader) {
            commonPrefixGrouper.scanForGroups(itemReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String item : yamlConfig.getConfig().getPrefixGrouper().getBaseLine()) {
            LOG.info("Adding baseline item '" + item + "'");
            commonPrefixGrouper.learnLine(item);
        }

        return commonPrefixGrouper;
    }

}
