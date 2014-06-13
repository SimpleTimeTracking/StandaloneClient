package org.stt;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple configuration mechanism with fallback values.
 * 
 * Resolves environment variables like HOME if enclosed in $ signs like so:
 * $HOME$/.stt
 */
public class Configuration {

	private final File propertiesFile = new File(
			System.getProperty("user.home"), ".sttrc");

	private static Configuration instance = null;

	private Properties loadedProps;

	private static final Pattern ENV_PATTERN = Pattern
			.compile(".*\\$(.*)\\$.*");

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
		String sttFile = getPropertiesReplaced("sttFile", "$HOME$/.stt");
		return new File(sttFile);
	}

	public File getTiFile() {
		String tiFile = getPropertiesReplaced("tiFile", "$HOME$/.ti-sheet");
		return new File(tiFile);
	}

	public File getTiCurrentFile() {
		String tiFile = getPropertiesReplaced("tiCurrentFile",
				"$HOME$/.ti-sheet-current");
		return new File(tiFile);
	}

	private String getPropertiesReplaced(String propName, String fallback) {
		String theProperty = loadedProps.getProperty(propName, fallback);
		Matcher envMatcher = ENV_PATTERN.matcher(theProperty);
		if (envMatcher.find()) {
			String group = envMatcher.group(1);
			String getenv = System.getenv(group);
			if (getenv != null) {
				theProperty = theProperty.replace("$" + group + "$", getenv);
			}
		}
		return theProperty;

	}
}
