package org.stt.config

import com.jsoniter.JsonIterator
import com.jsoniter.output.JsonStream
import com.jsoniter.spi.JsoniterSpi
import net.sf.json.JSONException
import org.stt.Service
import org.stt.time.DateTimes
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.time.Duration
import java.time.LocalTime
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class JsonConfigService @Inject
constructor(@Named("homePath") homePath: String) : ConfigService, Service {
    private val sttJson: File
    override lateinit var config: ConfigRoot
    var isNewConfig: Boolean = false
        private set

    init {
        sttJson = File("$homePath/.stt", "stt.json")
    }

    override fun start() {
        val mkdirs = sttJson.parentFile.mkdirs()
        if (mkdirs) {
            LOG.finest("Created base dir.")
        }
        JsonStream.setIndentionStep(3)
        JsoniterSpi.registerTypeDecoder(Duration::class.java) { d ->
            val durationString = d.readString()
            Duration.between(LocalTime.MIDNIGHT, LocalTime.parse(durationString,
                    DateTimes.DATE_TIME_FORMATTER_HH_MM_SS))
        }
        JsoniterSpi.registerTypeDecoder(PathSetting::class.java) { d -> PathSetting(d.readString()) }
        JsoniterSpi.registerTypeDecoder(PasswordSetting::class.java) { d ->
            val encodedPassword = d.readString() ?: return@registerTypeDecoder null
            PasswordSetting.fromEncryptedPassword(Base64.getDecoder().decode(encodedPassword))
        }

        JsoniterSpi.registerTypeEncoder(PathSetting::class.java) { o, s -> s.writeVal((o as PathSetting).path()) }
        JsoniterSpi.registerTypeEncoder(PasswordSetting::class.java) { o, s -> s.writeVal(Base64.getEncoder().encodeToString((o as PasswordSetting).encodedPassword)) }
        JsoniterSpi.registerTypeEncoder(Duration::class.java) { o, s ->
            val duration = o as Duration
            val asLocalTime = LocalTime.MIDNIGHT.plus(duration)
            s.writeVal(DateTimes.DATE_TIME_FORMATTER_HH_MM_SS.format(asLocalTime))
        }

        try {
            LOG.info("Loading " + sttJson.name)
            config = JsonIterator.deserialize(Files.readAllBytes(sttJson.toPath()), ConfigRoot::class.java)
        } catch (e: InvalidPathException) {
            LOG.log(Level.FINEST, "No previous config file found, creating a new one.", e)
            createNewConfig()
        } catch (e: IOException) {
            LOG.log(Level.FINEST, "No previous config file found, creating a new one.", e)
            createNewConfig()
        } catch (e: JSONException) {
            LOG.log(Level.FINEST, "No previous config file found, creating a new one.", e)
            createNewConfig()
        }

    }

    private fun createNewConfig() {
        LOG.info("Creating new config")
        config = ConfigRoot()
        isNewConfig = true
    }

    override fun stop() {
        writeConfig()
    }

    private fun writeConfig() {
        LOG.info("Writing config to " + sttJson.name)
        try {
            Files.write(sttJson.toPath(), JsonStream.serialize(config).toByteArray(charset("UTF-8")))
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, null, e)
        } catch (e: JSONException) {
            LOG.log(Level.SEVERE, null, e)
        }

    }

    companion object {
        private val LOG = Logger.getLogger(JsonConfigService::class.java
                .name)
    }
}
