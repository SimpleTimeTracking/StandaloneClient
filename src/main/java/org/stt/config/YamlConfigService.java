package org.stt.config;

import org.stt.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;


@Singleton
public class YamlConfigService implements Service {

    private static final Logger LOG = Logger.getLogger(YamlConfigService.class
            .getName());
    private final File sttYaml;
    private ConfigRoot config;

    @Inject
    public YamlConfigService() {
        sttYaml = new File(determineBaseDir(), "stt.yaml");
    }

    private static File determineBaseDir() {
        String envHOMEVariable = System.getenv("HOME");
        if (envHOMEVariable != null) {
            File homeDirectory = new File(envHOMEVariable);
            if (homeDirectory.exists()) {
                return homeDirectory;
            }
        }
        return new File(System.getProperty("user.home"));
    }

    private void writeConfig() {
        LOG.info("Writing config to " + sttYaml.getName());
        try (FileOutputStream out = new FileOutputStream(sttYaml);
             Writer writer = new OutputStreamWriter(out, "UTF8")) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(FlowStyle.BLOCK);
            new Yaml(options).dump(config, writer);
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
            Constructor constructor = new Constructor(ConfigRoot.class);
            PropertyUtils propertyUtils = new PropertyUtils();
            propertyUtils.setSkipMissingProperties(true);
            constructor.setPropertyUtils(propertyUtils);
            Yaml yaml = new Yaml(constructor);
            config = (ConfigRoot) yaml.load(fileInputStream);
        } catch (FileNotFoundException e) {
            LOG.log(Level.FINEST, "No previous config file found, creating a new one.", e);
            createNewConfig();
        } catch (IOException | ClassCastException | NullPointerException ex) {
            LOG.log(Level.SEVERE, null, ex);
            createNewConfig();
        }
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
}
