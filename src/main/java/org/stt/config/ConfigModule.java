package org.stt.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Created by dante on 26.06.15.
 */
public class ConfigModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    @Singleton
    private TimeTrackingItemListConfig provideTimeTrackingItemListConfig(YamlConfigService yamlConfigService) {
        return yamlConfigService.getConfig().getTimeTrackingItemListConfig();
    }

    @Provides
    @Singleton
    private CommandTextConfig provideCommandTextConfig(YamlConfigService yamlConfigService) {
        return yamlConfigService.getConfig().getCommandText();
    }

}
