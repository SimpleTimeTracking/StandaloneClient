package org.stt.cli;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;

public class MainTest {
	private Main sut;

	@Mock
	private Configuration configuration;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File currentSttFile;
	private File currentTiFile;
	private File currentTiCurrentFile;

	@Before
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);

		currentSttFile = tempFolder.newFile();
		Mockito.when(configuration.getSttFile()).thenReturn(currentSttFile);
		currentTiFile = tempFolder.newFile();
		Mockito.when(configuration.getTiFile()).thenReturn(currentTiFile);
		currentTiCurrentFile = tempFolder.newFile();
		Mockito.when(configuration.getTiCurrentFile()).thenReturn(
				currentTiCurrentFile);

		sut = new Main(configuration);
	}

	@Test
	public void startingWorkWritesToConfiguredFile() throws IOException {

		// GIVEN
		String expectedComment = "some long comment we are currently working on";
		List<String> args = new ArrayList<>();
		String command = "on " + expectedComment;
		args.addAll(Arrays.asList(command.split(" ")));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, "UTF-8");

		// WHEN
		sut.executeCommand(args, ps);

		// THEN
		List<String> readLines = IOUtils.readLines(new InputStreamReader(
				new FileInputStream(currentSttFile), "UTF-8"));
		Assert.assertThat(readLines, contains(containsString(expectedComment)));

		String returned = baos.toString("UTF-8");
		Assert.assertThat(returned, containsString(expectedComment));

		ps.close();
	}

}
