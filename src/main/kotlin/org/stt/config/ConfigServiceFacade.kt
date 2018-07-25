package org.stt.config

import org.stt.Service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigServiceFacade @Inject
constructor(private val yamlConfigService: YamlConfigService, private val jsonConfigService: JsonConfigService) : ConfigService, Service {
    private var skipYaml: Boolean = false

    override val config: ConfigRoot
        get() = if (skipYaml) jsonConfigService.config else yamlConfigService.config

    override fun start() {
        jsonConfigService.start()
        if (jsonConfigService.isNewConfig) {
            yamlConfigService.start()
            jsonConfigService.config = yamlConfigService.config
        } else {
            skipYaml = true
        }
    }

    override fun stop() {
        jsonConfigService.stop()
        if (!skipYaml) {
            yamlConfigService.stop()
        }
    }
}
