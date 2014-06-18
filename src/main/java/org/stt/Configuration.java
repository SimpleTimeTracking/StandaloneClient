package org.stt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
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
	private static final Logger LOG = Logger.getLogger(Configuration.class
			.getName());

	private final Properties loadedProps;

	private final File baseDir;

	private static final Pattern ENV_PATTERN = Pattern
			.compile(".*\\$(.*)\\$.*");

	public Configuration() {
		baseDir = determineBaseDir();

		loadedProps = new Properties();
		File propertiesFile = getPropertiesFile();
		if (propertiesFile.exists()) {
			try (Reader propsReader = new InputStreamReader(
					new FileInputStream(propertiesFile), "UTF-8")) {
				loadedProps.load(propsReader);
			} catch (IOException e) {
				// if the config file cannot be read, defaults are used
				LOG.log(Level.WARNING, "cannot read config file "
						+ propertiesFile.getAbsolutePath(), e);
			}
		} else {
			// create the file from example
			createSttrc();
		}
	}

	private File determineBaseDir() {
		String envHOMEVariable = System.getenv("HOME");
		if (envHOMEVariable != null) {
			File homeDirectory = new File(envHOMEVariable);
			if (homeDirectory.exists()) {
				return homeDirectory;
			}
		}
		return new File(System.getProperty("user.home"));
	}

	private void createSttrc() {

		File propertiesFile = getPropertiesFile();
		try (InputStream rcStream = this.getClass().getResourceAsStream(
				"/org/stt/sttrc.example")) {
			IOUtils.copy(rcStream, new FileOutputStream(propertiesFile));
		} catch (IOException e) {
			LOG.log(Level.WARNING, "cannot write example config file "
					+ propertiesFile.getAbsolutePath(), e);
		}
	}

	private File getPropertiesFile() {
		return new File(baseDir, ".sttrc");
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
			if ("HOME".equals(group)) {
				theProperty = theProperty.replace("$" + group + "$",
						baseDir.getAbsolutePath());
			}
		}
		return theProperty;
	}
}
