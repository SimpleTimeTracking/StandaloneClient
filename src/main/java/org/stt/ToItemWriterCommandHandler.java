package org.stt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

public class ToItemWriterCommandHandler implements CommandHandler {

	private final ItemWriter itemWriter;

	public ToItemWriterCommandHandler(ItemWriter itemWriter) {
		this.itemWriter = checkNotNull(itemWriter);
	}

	@Override
	public void executeCommand(String command) {
		checkNotNull(command);
		try {
			itemWriter.write(new TimeTrackingItem(command, DateTime.now()));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void close() throws IOException {
		itemWriter.close();
	}
}
