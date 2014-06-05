package org.stt.importer.ti;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;

import org.apache.commons.io.IOUtils;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.google.common.base.Preconditions;

/**
 * Writes {@link TimeTrackingItem}s to a new line.
 * Multiline comments get joined into one line: line endings \r and \n get replaced by the string \r and \n respectively.
 *
 */
public class DefaultItemExporter implements ItemWriter {

	private Writer outputWriter;
	
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
	
	private static final String EOL = System.getProperty("line.separator");
	
	public DefaultItemExporter(Writer output) {
		outputWriter = output;
	}
	
	@Override
	public void write(TimeTrackingItem item) throws IOException {
		Preconditions.checkNotNull(item);

		outputWriter.write(EOL);
		outputWriter.write(dateFormat.format(item.getStart().getTime()));
		outputWriter.write(' ');
		if(item.getEnd().isPresent()) {
			outputWriter.write(dateFormat.format(item.getEnd().get().getTime()));
			outputWriter.write(' ');
		}
		
		if(item.getComment().isPresent()) {
			String oneLineComment = item.getComment().get();
			oneLineComment = oneLineComment.replaceAll("\r", "\\\\r");
			oneLineComment = oneLineComment.replaceAll("\n", "\\\\n");
			outputWriter.write(oneLineComment);
		}
	}

	@Override
	public void delete(TimeTrackingItem item) {
		//FIXME: implement ;-)
		throw new NotImplementedException();
	}

	@Override
	public void close() {
		IOUtils.closeQuietly(outputWriter);
	}

}
