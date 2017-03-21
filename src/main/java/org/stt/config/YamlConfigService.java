package org.stt.config;

import org.stt.Service;
import org.stt.time.DateTimes;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;


@Singleton
public class YamlConfigService implements Service {
    static final Tag TAG_DURATION = new Tag("!duration");
    static final Tag TAG_ENCRYPTED = new Tag("!encrypted");
    static final Tag TAG_PATH = new Tag("!path");
    private static final Logger LOG = Logger.getLogger(YamlConfigService.class
            .getName());
    private final File sttYaml;
    private ConfigRoot config;

    @Inject
    public YamlConfigService(@Named("homePath") String homePath) {
        sttYaml = new File(homePath + "/.stt", "stt.yaml");
        boolean mkdirs = sttYaml.getParentFile().mkdirs();
        if (mkdirs) {
            LOG.finest("Created base dir.");
        }
    }

    private void writeConfig() {
        LOG.info("Writing config to " + sttYaml.getName());
        try (FileOutputStream out = new FileOutputStream(sttYaml);
             Writer writer = new OutputStreamWriter(out, "UTF8")) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(FlowStyle.BLOCK);
            yaml().dump(config, writer);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public ConfigRoot getConfig() {
        return config;
    }

    @Override
    public void start() throws Exception {
        try (FileInputStream fileInputStream = new FileInputStream(sttYaml)) {
            LOG.info("Loading " + sttYaml.getName());
            Yaml yaml = yaml();
            config = (ConfigRoot) yaml.load(fileInputStream);
        } catch (FileNotFoundException e) {
            LOG.log(Level.FINEST, "No previous config file found, creating a new one.", e);
            createNewConfig();
        } catch (IOException | ClassCastException | NullPointerException ex) {
            LOG.log(Level.SEVERE, null, ex);
            createNewConfig();
        }
    }

    private Yaml yaml() {
        return new Yaml(new MyConstructor(), new MyRepresenter());
    }

    private void createNewConfig() {
        LOG.info("Creating new config");
        config = new ConfigRoot();
    }

    @Override
    public void stop() {
        // Overwrite existing config, some new options might be available, or old ones removed.
        writeConfig();
    }

    private class MyConstructor extends Constructor {

        public MyConstructor() {
            super(ConfigRoot.class);

            PropertyUtils propertyUtils = new PropertyUtils();
            propertyUtils.setSkipMissingProperties(true);
            setPropertyUtils(propertyUtils);

            yamlConstructors.put(TAG_DURATION, new AbstractConstruct() {
                @Override
                public Object construct(Node node) {
                    String durationString = (String) constructScalar((ScalarNode) node);
                    return Duration.between(LocalTime.MIDNIGHT, LocalTime.parse(durationString,
                            DateTimes.DATE_TIME_FORMATTER_HH_MM_SS));
                }
            });
            yamlConstructors.put(TAG_PATH, new AbstractConstruct() {
                @Override
                public Object construct(Node node) {
                    String path = (String) constructScalar((ScalarNode) node);
                    return new PathSetting(path);
                }
            });
            yamlConstructors.put(TAG_ENCRYPTED, new AbstractConstruct() {
                @Override
                public Object construct(Node node) {
                    String base64EncryptedPassword = (String) constructScalar((ScalarNode) node);
                    try {
                        return PasswordSetting.fromEncryptedPassword(Base64.getDecoder().decode(base64EncryptedPassword));
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, String.format("Invalid encrypted string at %s", node.getNodeId()), e);
                    }
                    return null;
                }
            });
        }
    }

    private static class MyRepresenter extends Representer {

        MyRepresenter() {
            representers.put(Duration.class, data -> {
                Duration duration = (Duration) data;
                LocalTime asLocalTime = LocalTime.MIDNIGHT.plus(duration);
                return representScalar(TAG_DURATION, DateTimes.DATE_TIME_FORMATTER_HH_MM_SS.format(asLocalTime));
            });
            representers.put(PathSetting.class, data -> representScalar(TAG_PATH, ((PathSetting) data).path()));
            representers.put(PasswordSetting.class, data -> representScalar(TAG_ENCRYPTED, Base64.getEncoder().encodeToString(((PasswordSetting) data).encodedPassword)));
            addClassTag(ConfigRoot.class, Tag.MAP);
        }
    }
}
