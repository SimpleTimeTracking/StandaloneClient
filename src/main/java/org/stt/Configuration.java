package org.stt;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Configuration {

	private final File propertiesFile = new File(
			System.getProperty("user.home"), ".sttrc");

	private static Configuration instance = null;

	private Properties loadedProps;

	private Configuration() {
		loadedProps = new Properties();
		try {
			loadedProps.load(new FileReader(propertiesFile));
		} catch (IOException e) {
			// NOOP if the config file is not present, use the given defaults
		}
	}

	public static Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}

	public File getSttFile() {
		String sttFile = loadedProps.getProperty("sttFile",
				System.getProperty("user.home") + "/.stt");
		return new File(sttFile);
	}

	public File getTiFile() {
		String tiFile = loadedProps.getProperty("tiFile",
				System.getProperty("user.home") + "/.ti-sheet");
		return new File(tiFile);
	}

	public File getTiCurrentFile() {
		String tiFile = loadedProps.getProperty("tiCurrentFile",
				System.getProperty("user.home") + "/.ti-sheet-current");
		return new File(tiFile);
	}
}
