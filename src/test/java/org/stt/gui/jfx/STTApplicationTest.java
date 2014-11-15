package org.stt.gui.jfx;

import com.google.common.base.Optional;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import org.joda.time.DateTime;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.stt.CommandHandler;
import org.stt.gui.jfx.STTApplication.Builder;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.ItemGrouper;
import org.stt.searching.CommentSearcher;
import org.stt.searching.ExpansionProvider;

public class STTApplicationTest {

	private STTApplication sut;

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

	@Mock
	private ResourceBundle resourceBundle;

	private boolean shutdownCalled;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		ItemReaderProvider historySourceProvider = mock(ItemReaderProvider.class);
		Builder builder = new Builder();
		builder.commandHandler(commandHandler)
				.historySourceProvider(historySourceProvider)
				.executorService(executorService)
				.reportWindowBuilder(reportWindowBuilder)
				.expansionProvider(expansionProvider)
				.resourceBundle(resourceBundle);
		sut = builder.build();
		sut.viewAdapter
				= sut.new ViewAdapter(


					null) {

            @Override
					protected void show() throws RuntimeException {
					}

					@Override
					protected void requestFocusOnCommandText() {
					}

					@Override
					protected void updateAllItems(Collection<TimeTrackingItem> updateWith) {
						sut.allItems.setAll(updateWith);
					}

					@Override
					protected void shutdown() {
						shutdownCalled = true;
					}
				};
	}

	@Test
	public void shouldDelegateToExpansionProvider() {
		// GIVEN

		setTextAndPositionCaretAtEnd("test");

		given(expansionProvider.getPossibleExpansions("test")).willReturn(
				Arrays.asList("blub"));

		// WHEN
		sut.expandCurrentCommand();

		// THEN
		assertThat(sut.currentCommand.get(), is("testblub"));
	}

	@Test
	public void shouldExpandWithinText() {
		// GIVEN

		sut.currentCommand.set("al beta");
		sut.commandCaretPosition.set(2);

		given(expansionProvider.getPossibleExpansions("al")).willReturn(
				Arrays.asList("pha"));

		// WHEN
		sut.expandCurrentCommand();

		// THEN
		assertThat(sut.currentCommand.get(), is("alpha beta"));
		assertThat(sut.commandCaretPosition.get(), is(5));
	}

	@Test
	public void shouldExpandToCommonPrefix() {
		// GIVEN

		String currentText = "test";
		setTextAndPositionCaretAtEnd(currentText);

		given(expansionProvider.getPossibleExpansions(currentText)).willReturn(
				Arrays.asList("aaa", "aab"));

		// WHEN
		sut.expandCurrentCommand();

		// THEN
		assertThat(sut.currentCommand.get(), is("testaa"));
	}

	private void setTextAndPositionCaretAtEnd(String currentText) {
		sut.currentCommand.set(currentText);
		sut.commandCaretPosition.set(currentText.length());
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

		sut.allItems.setAll(item);

		// WHEN
		sut.delete(item);

		// THEN
		assertThat(sut.filteredList.get(), not(hasItem(item)));
	}

	@Test
	public void shouldShowReportWindow() throws IOException {
        // GIVEN

		// WHEN
		sut.viewAdapter.showReportWindow();

		// THEN
		verify(reportWindowBuilder).setupStage();
	}

	@Test
	public void shouldClearCommandAreaOnExecuteCommand() throws Exception {
		// GIVEN
		givenCommand("test");

		// WHEN
		sut.executeCommand();

		// THEN
		assertThat(sut.currentCommand.get(), equalTo(""));
	}

	@Test
	public void shouldDelegateCommandExecutionToCommandHandler()
			throws Exception {
		// GIVEN
		String testCommand = "test";

		givenCommand(testCommand);

		// WHEN
		sut.executeCommand();

		// THEN
		verify(commandHandler).executeCommand(testCommand);
	}

	@Test
	public void shouldNotCloseWindowOnInsert() {
		// GIVEN
		givenCommand("Hello World");
		given(commandHandler.executeCommand(anyString())).willReturn(Optional.<TimeTrackingItem>absent());

		// WHEN
		sut.viewAdapter.insert();

		// THEN
		assertThat(shutdownCalled, is(false));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldReadHistoryItemsFromReader() throws Exception {
		// GIVEN
		givenExecutorService();

		final TimeTrackingItem item = new TimeTrackingItem("comment",
				DateTime.now());

		ItemReader reader = givenReaderThatReturns(item);

		// WHEN
		sut.readHistoryFrom(reader);

		// THEN
		verify(executorService).execute(any(Runnable.class));
		assertThat(
				sut.filteredList.get().toArray(new TimeTrackingItem[0]),
				is(new TimeTrackingItem[]{item}));
	}

	private ItemReader givenReaderThatReturns(final TimeTrackingItem item) {
		ItemReader reader = mock(ItemReader.class);
		given(reader.read()).willReturn(Optional.of(item),
				Optional.<TimeTrackingItem>absent());
		return reader;
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
		sut.currentCommand.set(command);
	}
}
