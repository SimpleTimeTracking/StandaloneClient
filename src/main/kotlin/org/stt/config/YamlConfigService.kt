package org.stt.config

import org.stt.Service
import org.stt.time.DateTimes
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.DumperOptions.FlowStyle
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.AbstractConstruct
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.introspector.PropertyUtils
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer
import java.io.*
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalTime
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


@Singleton
class YamlConfigService @Inject
constructor(@Named("homePath") homePath: String) : Service, ConfigService {
    private val sttYaml: File
    override lateinit var config: ConfigRoot
        private set

    init {
        sttYaml = File("$homePath/.stt", "stt.yaml")
    }

    private fun writeConfig() {
        LOG.info("Writing config to " + sttYaml.name)
        try {
            FileOutputStream(sttYaml).use { out ->
                OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
                    val options = DumperOptions()
                    options.defaultFlowStyle = FlowStyle.BLOCK
                    yaml().dump(config, writer)
                }
            }
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    @Throws(Exception::class)
    override fun start() {
        val mkdirs = sttYaml.parentFile.mkdirs()
        if (mkdirs) {
            LOG.finest("Created base dir.")
        }

        try {
            FileInputStream(sttYaml).use { fileInputStream ->
                LOG.info("Loading " + sttYaml.name)
                val yaml = yaml()
                config = yaml.load<Any>(fileInputStream) as ConfigRoot
            }
        } catch (e: FileNotFoundException) {
            LOG.log(Level.FINEST, "No previous config file found, creating a new one.", e)
            createNewConfig()
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
            createNewConfig()
        } catch (ex: ClassCastException) {
            LOG.log(Level.SEVERE, null, ex)
            createNewConfig()
        } catch (ex: NullPointerException) {
            LOG.log(Level.SEVERE, null, ex)
            createNewConfig()
        }

    }

    private fun yaml(): Yaml {
        return Yaml(MyConstructor(), MyRepresenter())
    }

    private fun createNewConfig() {
        LOG.info("Creating new config")
        config = ConfigRoot()
    }

    override fun stop() {
        // Overwrite existing config, some new options might be available, or old ones removed.
        writeConfig()
    }

    private class MyConstructor : Constructor(ConfigRoot::class.java) {
        init {

            val propertyUtils = PropertyUtils()
            propertyUtils.isSkipMissingProperties = true
            setPropertyUtils(propertyUtils)

            yamlConstructors[TAG_DURATION] = object : AbstractConstruct() {
                override fun construct(node: Node): Any {
                    val durationString = constructScalar(node as ScalarNode) as String
                    return Duration.between(LocalTime.MIDNIGHT, LocalTime.parse(durationString,
                            DateTimes.DATE_TIME_FORMATTER_HH_MM_SS))
                }
            }
            yamlConstructors[TAG_PATH] = object : AbstractConstruct() {
                override fun construct(node: Node): Any {
                    val path = constructScalar(node as ScalarNode) as String
                    return PathSetting(path)
                }
            }
            yamlConstructors[TAG_ENCRYPTED] = object : AbstractConstruct() {
                override fun construct(node: Node): Any? {
                    val base64EncryptedPassword = constructScalar(node as ScalarNode) as String
                    try {
                        return PasswordSetting.fromEncryptedPassword(Base64.getDecoder().decode(base64EncryptedPassword))
                    } catch (e: Exception) {
                        LOG.log(Level.SEVERE, String.format("Invalid encrypted string at %s", node.getNodeId()), e)
                    }

                    return null
                }
            }
        }
    }

    private class MyRepresenter internal constructor() : Representer() {

        init {
            representers[Duration::class.java] = Represent { data ->
                val duration = data as Duration
                val asLocalTime = LocalTime.MIDNIGHT.plus(duration)
                representScalar(TAG_DURATION, DateTimes.DATE_TIME_FORMATTER_HH_MM_SS.format(asLocalTime))
            }
            representers[PathSetting::class.java] = Represent { data -> representScalar(TAG_PATH, (data as PathSetting).path()) }
            representers[PasswordSetting::class.java] = Represent { data -> representScalar(TAG_ENCRYPTED, Base64.getEncoder().encodeToString((data as PasswordSetting).encodedPassword)) }
            addClassTag(ConfigRoot::class.java, Tag.MAP)
        }
    }

    companion object {
        internal val TAG_DURATION = Tag("!duration")
        internal val TAG_ENCRYPTED = Tag("!encrypted")
        internal val TAG_PATH = Tag("!path")
        private val LOG = Logger.getLogger(YamlConfigService::class.java.name)
    }
}
