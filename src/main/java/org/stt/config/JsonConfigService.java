package org.stt.config;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsoniterSpi;
import net.sf.json.JSONException;
import org.stt.Service;
import org.stt.time.DateTimes;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class JsonConfigService implements ConfigService, Service {
    private static final Logger LOG = Logger.getLogger(JsonConfigService.class
            .getName());
    private final File sttJson;
    private ConfigRoot config;
    private boolean newConfig;

    @Inject
    public JsonConfigService(@Named("homePath") String homePath) {
        sttJson = new File(homePath + "/.stt", "stt.json");
    }

    public boolean isNewConfig() {
        return newConfig;
    }

    public ConfigRoot getConfig() {
        return config;
    }

    @Override
    public void start() throws Exception {
        boolean mkdirs = sttJson.getParentFile().mkdirs();
        if (mkdirs) {
            LOG.finest("Created base dir.");
        }
        JsonStream.setIndentionStep(3);
        JsoniterSpi.registerTypeDecoder(Duration.class, d -> {
            String durationString = d.readString();
            return Duration.between(LocalTime.MIDNIGHT, LocalTime.parse(durationString,
                    DateTimes.DATE_TIME_FORMATTER_HH_MM_SS));
        });
        JsoniterSpi.registerTypeDecoder(PathSetting.class, d -> new PathSetting(d.readString()));
        JsoniterSpi.registerTypeDecoder(PasswordSetting.class, d -> {
            String encodedPassword = d.readString();
            if (encodedPassword == null) {
                return null;
            }
            return PasswordSetting.fromEncryptedPassword(Base64.getDecoder().decode(encodedPassword));
        });

        JsoniterSpi.registerTypeEncoder(PathSetting.class, (o, s) -> s.writeVal(((PathSetting) o).path()));
        JsoniterSpi.registerTypeEncoder(PasswordSetting.class, (o, s) -> s.writeVal(Base64.getEncoder().encodeToString(((PasswordSetting) o).encodedPassword)));
        JsoniterSpi.registerTypeEncoder(Duration.class, (o, s) -> {
            Duration duration = (Duration) o;
            LocalTime asLocalTime = LocalTime.MIDNIGHT.plus(duration);
            s.writeVal(DateTimes.DATE_TIME_FORMATTER_HH_MM_SS.format(asLocalTime));
        });

        try {
            LOG.info("Loading " + sttJson.getName());
            config = JsonIterator.deserialize(Files.readAllBytes(sttJson.toPath()), ConfigRoot.class);
        } catch (InvalidPathException | IOException | JSONException e) {
            LOG.log(Level.FINEST, "No previous config file found, creating a new one.", e);
            createNewConfig();
        }
    }

    private void createNewConfig() {
        LOG.info("Creating new config");
        config = new ConfigRoot();
        newConfig = true;
    }

    @Override
    public void stop() {
        writeConfig();
    }

    private void writeConfig() {
        LOG.info("Writing config to " + sttJson.getName());
        try {
            Files.write(sttJson.toPath(), JsonStream.serialize(config).getBytes("UTF-8"));
        } catch (IOException | JSONException e) {
            LOG.log(Level.SEVERE, null, e);
        }
    }
}
