package org.stt.cli;

import java.util.Collection;

public class StringHelper {

	public static String join(Collection<String> args) {

		StringBuilder builder = new StringBuilder();
		String separator = "";
		for (String s : args) {
			builder.append(separator);
			builder.append(s);
			separator = " ";
		}
		return builder.toString();
	}
}
