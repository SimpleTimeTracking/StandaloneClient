package org.stt;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.*;
import org.stt.config.BaseConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

/**
 *
 * @author dante
 */
@com.google.inject.Singleton
public class YamlConfig {

	private static final Logger LOG = Logger.getLogger(YamlConfig.class
			.getName());

	private BaseConfig config;

	public YamlConfig() {
		final File sttYaml = new File(determineBaseDir(), "stt.yaml");
		try (FileInputStream fileInputStream = new FileInputStream(sttYaml)) {
			Constructor constructor = new Constructor(BaseConfig.class);
			PropertyUtils propertyUtils = new PropertyUtils();
			propertyUtils.setSkipMissingProperties(true);
			constructor.setPropertyUtils(propertyUtils);
			Yaml yaml = new Yaml(constructor);
			config = (BaseConfig) yaml.load(fileInputStream);
			config.applyDefaults();
		} catch (FileNotFoundException e) {
			config = new BaseConfig();
			config.applyDefaults();
		} catch (IOException | ClassCastException ex) {
			LOG.log(Level.SEVERE, null, ex);
			config = new BaseConfig();
			config.applyDefaults();
		}
		// Overwrite existing config, some new options might be available, or old ones removed.
		writeConfig(sttYaml);
	}

	private void writeConfig(File sttYaml) {
		try (FileOutputStream out = new FileOutputStream(sttYaml);
                Writer writer = new OutputStreamWriter(out, "UTF8")) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(FlowStyle.BLOCK);
            new Yaml(options).dump(config, writer);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
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

	public BaseConfig getConfig() {
		return config;
	}
}
