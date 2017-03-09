package org.stt.config;


import dagger.Module;
import dagger.Provides;

@Module
public class ConfigModule {
    private ConfigModule() {
    }

    @Provides
    static TimeTrackingItemListConfig provideTimeTrackingItemListConfig(YamlConfigService yamlConfigService) {
        return yamlConfigService.getConfig().getTimeTrackingItemListConfig();
    }

    @Provides
    static CommandTextConfig provideCommandTextConfig(YamlConfigService yamlConfigService) {
        return yamlConfigService.getConfig().getCommandText();
    }

}
