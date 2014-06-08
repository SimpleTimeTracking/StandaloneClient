package org.stt;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.stt.gui.Main;

public class StartWithJFX {
	public static void main(String[] args) throws MalformedURLException,
			IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, ClassNotFoundException,
			NoSuchMethodException, SecurityException {
		try {
			Class.forName("javafx.embed.swing.JFXPanel");
		} catch (ClassNotFoundException e) {
			File jfxrt = retrieveJFXRTFile();
			URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader
					.getSystemClassLoader();
			addUrlToURLClassLoader(jfxrt, systemClassLoader);
		}
		Main.main(args);
	}

	private static void addUrlToURLClassLoader(File jfxrt,
			URLClassLoader systemClassLoader) throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException,
			MalformedURLException {
		Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL",
				URL.class);
		addUrlMethod.setAccessible(true);
		addUrlMethod.invoke(systemClassLoader, jfxrt.toURI().toURL());
		addUrlMethod.setAccessible(false);
	}

	private static File retrieveJFXRTFile() {
		File jfxrt = new File(System.getProperty("java.home")
				+ "/lib/jfxrt.jar");
		if (!jfxrt.exists()) {
			throw new IllegalStateException("Couldn't locate " + jfxrt);
		}
		return jfxrt;
	}
}
