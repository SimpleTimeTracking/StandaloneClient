package org.stt.config;

import org.stt.Service;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConfigServiceFacade implements ConfigService, Service {
    private final YamlConfigService yamlConfigService;
    private final JsonConfigService jsonConfigService;
    private boolean skipYaml;

    @Inject
    public ConfigServiceFacade(YamlConfigService yamlConfigService, JsonConfigService jsonConfigService) {
        this.yamlConfigService = yamlConfigService;
        this.jsonConfigService = jsonConfigService;
    }

    @Override
    public void start() throws Exception {
        jsonConfigService.start();
        if (jsonConfigService.isNewConfig()) {
            yamlConfigService.start();
        } else {
            skipYaml = true;
        }
    }

    @Override
    public void stop() {
        jsonConfigService.stop();
        if (!skipYaml) {
            yamlConfigService.stop();
        }
    }

    @Override
    public ConfigRoot getConfig() {
        return skipYaml ? jsonConfigService.getConfig() : yamlConfigService.getConfig();
    }
}
