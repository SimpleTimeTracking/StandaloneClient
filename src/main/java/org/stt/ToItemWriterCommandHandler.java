package org.stt;

import com.google.inject.Inject;
import org.stt.time.DateTimeHelper;
import java.io.IOException;
import java.util.logging.Logger;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.g4.EnglishCommandsLexer;
import org.stt.g4.EnglishCommandsParser;
import org.stt.g4.EnglishCommandsParser.CommandContext;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.searching.ItemSearcher;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class ToItemWriterCommandHandler implements CommandHandler {

	private static Logger LOG = Logger
			.getLogger(ToItemWriterCommandHandler.class.getName());

	static final DateTimeFormatter FORMAT_HOUR_MINUTES_SECONDS = DateTimeFormat
			.forPattern("HH:mm:ss");

	static final DateTimeFormatter FORMAT_YEAR_MONTH_HOUR_MINUTES_SECONDS = DateTimeFormat
			.forPattern("yyyy.MM.dd HH:mm:ss");

	public static final String COMMAND_FIN = "fin";

	private final ItemPersister itemWriter;
	private final ItemSearcher itemSearcher;

	@Inject
	public ToItemWriterCommandHandler(ItemPersister itemWriter,
			ItemSearcher itemSearcher) {
		this.itemWriter = checkNotNull(itemWriter);
		this.itemSearcher = checkNotNull(itemSearcher);
	}

	@Override
	public Optional<TimeTrackingItem> executeCommand(String command) {
		checkNotNull(command);
		CharStream inputStream;
		inputStream = new CaseInsensitiveInputStream(command);
		EnglishCommandsLexer lexer = new EnglishCommandsLexer(inputStream);
		TokenStream tokenStream = new CommonTokenStream(lexer);
		EnglishCommandsParser parser = new EnglishCommandsParser(tokenStream);
		CommandContext commandContext = parser.command();
		if (commandContext.newItem != null) {
			TimeTrackingItem parsedItem = commandContext.newItem;
			try {
				itemWriter.insert(parsedItem);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return Optional.of(parsedItem);
		}
		if (commandContext.fin != null) {
			return endCurrentItem(commandContext.fin);
		}
		return Optional.<TimeTrackingItem> absent();
	}

	@Override
	public void endCurrentItem() {
		endCurrentItem(DateTime.now());
	}

	@Override
	public Optional<TimeTrackingItem> endCurrentItem(DateTime endTime) {
		Optional<TimeTrackingItem> currentTimeTrackingitem = itemSearcher
				.getCurrentTimeTrackingitem();
		if (currentTimeTrackingitem.isPresent()) {
			TimeTrackingItem unfinisheditem = currentTimeTrackingitem.get();
			TimeTrackingItem nowFinishedItem = unfinisheditem.withEnd(endTime);
			try {
				itemWriter.replace(unfinisheditem, nowFinishedItem);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return Optional.of(nowFinishedItem);
		}
		return Optional.<TimeTrackingItem> absent();
	}

	@Override
	public void resumeGivenItem(TimeTrackingItem item) {
		TimeTrackingItem newItem = new TimeTrackingItem(
				item.getComment().get(), DateTime.now());
		try {
			itemWriter.insert(newItem);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		itemWriter.close();
	}

	@Override
	public String itemToCommand(TimeTrackingItem item) {
		checkNotNull(item);
		DateTimeFormatter formatForStart = getShortFormatForTodayAndLongOtherwise(item
				.getStart());

		StringBuilder builder = new StringBuilder(item.getComment().or(""));
		builder.append(' ');
		if (item.getEnd().isPresent()) {
			builder.append("from ");
			builder.append(formatForStart.print(item.getStart()));
			builder.append(" to ");
			DateTimeFormatter formatForEnd = getShortFormatForTodayAndLongOtherwise(item
					.getEnd().get());
			builder.append(formatForEnd.print(item.getEnd().get()));
		} else {
			builder.append("since ");
			builder.append(formatForStart.print(item.getStart()));
		}
		return builder.toString();
	}

	/**
	 * @return short time format for today and long format otherwise
	 */
	private DateTimeFormatter getShortFormatForTodayAndLongOtherwise(
			DateTime dateTime) {
		DateTimeFormatter formatForStart = FORMAT_YEAR_MONTH_HOUR_MINUTES_SECONDS;
		if (DateTimeHelper.isToday(dateTime)) {
			formatForStart = FORMAT_HOUR_MINUTES_SECONDS;
		}
		return formatForStart;
	}

	private static class CaseInsensitiveInputStream extends ANTLRInputStream {
		protected CaseInsensitiveInputStream(String input) {
			super(input);
		}

		@Override
		public int LA(int i) {
			int la = super.LA(i);
			if (Character.isAlphabetic(la)) {
				return Character.toLowerCase(la);
			}
			return la;
		}
	}

	@Override
	public void delete(TimeTrackingItem item) throws IOException {
		checkNotNull(item);
		itemWriter.delete(item);
	}
}
