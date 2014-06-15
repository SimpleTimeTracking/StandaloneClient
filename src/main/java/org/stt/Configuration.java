package org.stt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

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
		if (propertiesFile.exists()) {
			try {
				loadedProps.load(new FileReader(propertiesFile));
			} catch (IOException e) {
				// NOOP if the config file cannot be read, use the given
				// defaults
			}
		} else {
			// create the file from example
			createSttrc();
		}
	}

	private void createSttrc() {
		InputStream rcStream = this.getClass().getResourceAsStream(
				"/org/stt/sttrc.example");
		try {
			IOUtils.copy(rcStream, new FileOutputStream(propertiesFile));
		} catch (IOException e) {
			// NOOP if the file cannot be created, the defaults will be used
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

	public String getSystemOutEncoding() {
		String encoding = getPropertiesReplaced("sysoutEncoding", "UTF-8");
		return encoding;
	}

	public int getCliReportingWidth() {
		int encoding = Integer.parseInt(getPropertiesReplaced(
				"cliReportingWidth", "80"));
		return encoding;
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
