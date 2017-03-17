package org.stt.text;

import java.io.IOException;
import java.util.logging.Logger;

import org.stt.config.YamlConfigService;
import org.stt.persistence.ItemReader;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

/**
 * Created by dante on 04.12.14.
 */
public class TextModule extends AbstractModule {
    private static final Logger LOG = Logger.getLogger(TextModule.class.getName());

    @Override
    protected void configure() {
        bind(ItemGrouper.class).to(CommonPrefixGrouper.class);
        bind(ExpansionProvider.class).to(CommonPrefixGrouper.class);
        bind(ItemCategorizer.class).to(WorktimeCategorizer.class);

    	Multibinder<ExpansionProvider> expansionProviderBinder = Multibinder.newSetBinder(binder(), ExpansionProvider.class);
    	expansionProviderBinder.addBinding().to(CommonPrefixGrouper.class);
    	expansionProviderBinder.addBinding().to(JiraExpansionProvider.class);
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
