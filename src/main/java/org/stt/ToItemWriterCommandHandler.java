package org.stt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemSearcher;
import org.stt.persistence.ItemWriter;

import com.google.common.base.Optional;

public class ToItemWriterCommandHandler implements CommandHandler {

	private final ItemWriter itemWriter;
	private final ItemSearcher itemSearcher;

	public ToItemWriterCommandHandler(ItemWriter itemWriter,
			ItemSearcher itemSearcher) {
		this.itemWriter = checkNotNull(itemWriter);
		this.itemSearcher = checkNotNull(itemSearcher);
	}

	@Override
	public void executeCommand(String command) {
		checkNotNull(command);
		try {
			Optional<TimeTrackingItem> currentTimeTrackingitem = itemSearcher
					.getCurrentTimeTrackingitem();
			if (currentTimeTrackingitem.isPresent()) {
				TimeTrackingItem unfinisheditem = currentTimeTrackingitem.get();
				TimeTrackingItem nowFinishedItem = unfinisheditem
						.withEnd(DateTime.now());
				itemWriter.replace(unfinisheditem, nowFinishedItem);
			}
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
