package org.stt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.stt.config.BaseConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 *
 * @author dante
 */
public class YamlConfig {

	private static final Logger LOG = Logger.getLogger(YamlConfig.class
			.getName());

	private BaseConfig config;

	public YamlConfig() {
		final File sttYaml = new File(Configuration.determineBaseDir(),
				"stt.yaml");
		try (FileInputStream fileInputStream = new FileInputStream(sttYaml)) {
			Yaml yaml = new Yaml(new Constructor(BaseConfig.class));
			config = (BaseConfig) yaml.load(fileInputStream);
		} catch (FileNotFoundException e) {
			config = new BaseConfig();
			try (Writer writer = new FileWriter(sttYaml)) {
				DumperOptions options = new DumperOptions();
				options.setDefaultFlowStyle(FlowStyle.BLOCK);
				new Yaml(options).dump(config, writer);
			} catch (IOException ex) {
				LOG.log(Level.SEVERE, null, ex);
			}
		} catch (IOException | ClassCastException ex) {
			LOG.log(Level.SEVERE, null, ex);
		}
	}

	public BaseConfig getConfig() {
		return config;
	}
}
