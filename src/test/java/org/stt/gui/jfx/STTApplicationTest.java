package org.stt.gui.jfx;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import org.joda.time.DateTime;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.stt.CommandHandler;
import org.stt.gui.jfx.JFXTestRunner.NotOnPlatformThread;
import org.stt.gui.jfx.STTApplication.Builder;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.ItemGrouper;
import org.stt.searching.CommentSearcher;
import org.stt.searching.ExpansionProvider;

@Ignore(value = "JavaFX testing is unstable: The toolkit might shut down more or less anytime, unless a window is still open - or keep the test running forever")
@RunWith(JFXTestRunner.class)
public class STTApplicationTest {

	private STTApplication sut;
	private final JFXTestHelper helper = new JFXTestHelper();
	private Stage stage;

	@Mock
	private CommandHandler commandHandler;

	@Mock
	private ExecutorService executorService;

	@Mock
	private ReportWindowBuilder reportWindowBuilder;

	@Mock
	protected CommentSearcher commentSearcher;

	@Mock
	private ItemGrouper grouper;

	@Mock
	private ExpansionProvider expansionProvider;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				stage = helper.createStageForTest();
				ItemReaderProvider historySourceProvider = mock(ItemReaderProvider.class);
				Builder builder = new Builder();
				builder.stage(stage).commandHandler(commandHandler)
						.historySourceProvider(historySourceProvider)
						.executorService(executorService)
						.reportWindowBuilder(reportWindowBuilder)
						.expansionProvider(expansionProvider);
				sut = builder.build();
			}
		});
	}

	@Test
	@NotOnPlatformThread
	public void shouldDelegateToExpansionProvider() {
		// GIVEN
		setupStage();

		setTextAndPositionCaretAtEnd("test");

		given(expansionProvider.getPossibleExpansions("test")).willReturn(
				Arrays.asList("blub"));

		// WHEN
		sut.expandCurrentCommand();

		// THEN
		assertThat(sut.commandText.getText(), is("testblub"));
	}

	@Test
	@NotOnPlatformThread
	public void shouldExpandWithinText() {
		// GIVEN
		setupStage();

		sut.commandText.setText("al beta");
		sut.commandText.positionCaret(2);

		given(expansionProvider.getPossibleExpansions("al")).willReturn(
				Arrays.asList("pha"));

		// WHEN
		sut.expandCurrentCommand();

		// THEN
		assertThat(sut.commandText.getText(), is("alpha beta"));
		assertThat(sut.commandText.getCaretPosition(), is(5));
	}

	@Test
	@NotOnPlatformThread
	public void shouldExpandToCommonPrefix() {
		// GIVEN
		setupStage();

		String currentText = "test";
		setTextAndPositionCaretAtEnd(currentText);

		given(expansionProvider.getPossibleExpansions(currentText)).willReturn(
				Arrays.asList("aaa", "aab"));

		// WHEN
		sut.expandCurrentCommand();

		// THEN
		assertThat(sut.commandText.getText(), is("testaa"));
	}

	private void setTextAndPositionCaretAtEnd(String currentText) {
		sut.commandText.setText(currentText);
		sut.commandText.positionCaret(currentText.length());
	}

	@Test
	public void shouldDeleteItemIfRequested() throws IOException {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem(null, DateTime.now());

		// WHEN
		sut.delete(item);

		// THEN
		verify(commandHandler).delete(item);
	}

	@Test
	public void deletedItemShouldBeRemoved() {
		// GIVEN
		givenExecutorService();
		final TimeTrackingItem item = new TimeTrackingItem("comment",
				DateTime.now());
		setupStage();

		sut.allItems.setAll(item);

		// WHEN
		sut.delete(item);

		// THEN
		assertThat(sut.result.getItems(), not(hasItem(item)));
	}

	@Test
	public void shouldShowReportWindow() throws IOException {
		// GIVEN

		// WHEN
		sut.showReportWindow();

		// THEN
		verify(reportWindowBuilder).setupStage();
	}

	@Test
	public void shouldShowWindow() throws Exception {

		// GIVEN
		// WHEN
		sut.setupStage();

		// THEN
		assertThat(stage.isShowing(), is(true));
	}

	@Test
	public void shouldClearCommandAreaOnExecuteCommand() throws Exception {
		// GIVEN
		sut.setupStage();
		TextArea commandArea = getCommandArea();
		commandArea.setText("test");

		// WHEN
		sut.executeCommand();

		// THEN
		assertThat(commandArea.getText(), equalTo(""));
	}

	@Test
	public void shouldDelegateCommandExecutionToCommandHandler()
			throws Exception {
		// GIVEN
		String testCommand = "test";

		sut.setupStage();
		givenCommand(testCommand);

		// WHEN
		sut.executeCommand();

		// THEN
		verify(commandHandler).executeCommand(testCommand);
	}

	@Test
	public void shouldNotCloseWindowOnInsert() {
		// GIVEN
		sut.setupStage();
		givenCommand("Hello World");

		// WHEN
		sut.insert();

		// THEN
		assertThat(sut.stage.isShowing(), is(true));

	}

	@SuppressWarnings("unchecked")
	@Test
	@NotOnPlatformThread
	public void shouldReadHistoryItemsFromReader() throws Exception {
		// GIVEN
		givenExecutorService();

		final TimeTrackingItem item = new TimeTrackingItem("comment",
				DateTime.now());

		setupStage();

		ItemReader reader = givenReaderThatReturns(item);

		// WHEN
		sut.readHistoryFrom(reader);

		// THEN
		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				verify(executorService).execute(any(Runnable.class));
				assertThat(
						sut.result.getItems().toArray(new TimeTrackingItem[0]),
						is(new TimeTrackingItem[]{item}));
			}
		});
	}

	private ItemReader givenReaderThatReturns(final TimeTrackingItem item) {
		ItemReader reader = mock(ItemReader.class);
		given(reader.read()).willReturn(Optional.of(item),
				Optional.<TimeTrackingItem>absent());
		return reader;
	}

	private void setupStage() {
		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				try {
					sut.setupStage();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	private void givenExecutorService() {
		willAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				((Runnable) invocation.getArguments()[0]).run();
				return null;
			}
		}).given(executorService).execute(any(Runnable.class));
	}

	private void givenCommand(String command) {
		TextArea commandArea = getCommandArea();
		commandArea.setText(command);
	}

	private TextArea getCommandArea() {
		final TextArea commandArea = (TextArea) stage.getScene().lookup("#commandText");
		Assert.assertNotNull(commandArea);
		return commandArea;
	}
}
