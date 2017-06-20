package org.stt.cli;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockitoAnnotations;
import org.stt.command.Activities;
import org.stt.command.CommandFormatter;
import org.stt.command.CommandTextParser;
import org.stt.config.ConfigRoot;
import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReader;
import org.stt.persistence.stt.STTItemPersister;
import org.stt.persistence.stt.STTItemReader;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.WorkingtimeItemProvider;
import org.stt.text.ItemCategorizer;
import org.stt.text.WorktimeCategorizer;

import javax.inject.Provider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class MainTest {
	private Main sut;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

    private File currentSttFile;

	@Before
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);

        ConfigRoot configRoot = new ConfigRoot();

        currentSttFile = configRoot.getSttFile().file(tempFolder.newFolder().getAbsolutePath());
        boolean mkdirs = currentSttFile.getParentFile().mkdirs();
        assertThat(mkdirs, is(true));
        boolean newFile = currentSttFile.createNewFile();
        assertThat(newFile, is(true));

        Provider<Reader> sttReader = () -> {
            try {
                return new InputStreamReader(new FileInputStream(currentSttFile), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        Provider<Writer> sttWriter = () -> {
            try {
                return new OutputStreamWriter(new FileOutputStream(currentSttFile), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        Provider<ItemReader> readerProvider = () -> new STTItemReader(sttReader.get());
        TimeTrackingItemQueries queries = new TimeTrackingItemQueries(readerProvider, Optional.empty());
        WorkingtimeItemProvider worktimeItemProvider = new WorkingtimeItemProvider(configRoot.getWorktime(), "");
        ItemCategorizer categorizer = new WorktimeCategorizer(configRoot.getWorktime());
        ReportPrinter reportPrinter = new ReportPrinter(queries, configRoot.getCli(), worktimeItemProvider, categorizer);
        ItemPersister persister = new STTItemPersister(sttReader, sttWriter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        CommandFormatter commandFormatter = new CommandFormatter(new CommandTextParser(timeFormatter, dateTimeFormatter), dateTimeFormatter, timeFormatter);
        Activities activities = new Activities(persister, queries, Optional.empty());
        sut = new Main(queries, reportPrinter, commandFormatter, activities);
    }

	@Test
	public void startingWorkWritesToConfiguredFile() throws IOException {

		// GIVEN
		String expectedComment = "some long comment we are currently working on";
		List<String> args = new ArrayList<>();
		String command = "on " + expectedComment;
		args.addAll(Arrays.asList(command.split(" ")));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name());

		// WHEN
		sut.prepareAndExecuteCommand(args, ps);

		// THEN
		List<String> readLines = IOUtils.readLines(new InputStreamReader(
                new FileInputStream(currentSttFile), StandardCharsets.UTF_8));
        assertThat(readLines, contains(containsString(expectedComment)));

        String returned = baos.toString(StandardCharsets.UTF_8.name());
        assertThat(returned, containsString(expectedComment));

		ps.close();
	}

}
