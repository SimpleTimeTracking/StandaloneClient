package org.stt.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

import com.google.common.base.Preconditions;

/**
 * Writes {@link TimeTrackingItem}s to a new line. Multiline comments get joined
 * into one line: line endings \r and \n get replaced by the string \r and \n
 * respectively.
 * 
 */
public class DefaultItemExporter implements ItemWriter {

	private final StreamResourceProvider support;
	
	private final DateTimeFormatter dateFormat = DateTimeFormat
			.forPattern("yyyy-MM-dd_HH:mm:ss");

	private static final String EOL = System.getProperty("line.separator");

	public DefaultItemExporter(StreamResourceProvider support) {
		this.support = support;
	}

	@Override
	public void write(TimeTrackingItem item) throws IOException {
		Preconditions.checkNotNull(item);
		Writer writer = support.provideAppendingWriter();
		writer.write(EOL);
		writer.write(getWritableString(item));
	}

	private String getWritableString(TimeTrackingItem item)
			throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append(item.getStart().toString(dateFormat));
		builder.append(' ');
		if (item.getEnd().isPresent()) {
			builder.append(item.getEnd().get().toString(dateFormat));
			builder.append(' ');
		}

		if (item.getComment().isPresent()) {
			String oneLineComment = item.getComment().get();
			oneLineComment = oneLineComment.replaceAll("\r", "\\\\r");
			oneLineComment = oneLineComment.replaceAll("\n", "\\\\n");
			builder.append(oneLineComment);
		}
		
		return builder.toString();
	}

	@Override
	public void replace(TimeTrackingItem item, TimeTrackingItem with)
			throws IOException {
		delete(item);
		write(with);
	}

	@Override
	public void delete(TimeTrackingItem item) throws IOException {
		
		
		BufferedReader reader = new BufferedReader(support.provideReader());
		
		StringWriter stringWriter = new StringWriter();
		String currentLine = null;
		while((currentLine = reader.readLine()) != null) {
			if(currentLine.equals(getWritableString(item))) {
				//NOOP, do not write
			} else {
				stringWriter.write(EOL);
				stringWriter.write(currentLine);
			}
		}
		
		reader.close();
		Writer truncatingWriter = support.provideTruncatingWriter();
		truncatingWriter.write(stringWriter.toString());
		truncatingWriter.close();
	}

	@Override
	public void close() {
		IOUtils.closeQuietly(support);
	}

}
