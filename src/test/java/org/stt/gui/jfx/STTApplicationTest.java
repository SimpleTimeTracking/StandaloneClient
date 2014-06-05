package org.stt.gui.jfx;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.stt.CommandHandler;
import org.stt.gui.jfx.JFXTestRunner.NotOnPlatformThread;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

@RunWith(JFXTestRunner.class)
public class STTApplicationTest {
	private final STTApplication sut = new STTApplication();
	private final JFXTestHelper helper = new JFXTestHelper();
	private Stage stage;
	private CommandHandler commandHandler;

	@Before
	public void setup() {
		helper.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				stage = helper.createStageForTest();
				sut.setStage(stage);
				commandHandler = mock(CommandHandler.class);
				sut.setCommandHandler(commandHandler);
			}
		});
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

	@SuppressWarnings("unchecked")
	@Test
	@NotOnPlatformThread
	public void shouldReadHistoryItemsFromReader() throws Exception {
		// GIVEN
		final ExecutorService service = mock(ExecutorService.class);
		willAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				((Runnable) invocation.getArguments()[0]).run();
				return null;
			}
		}).given(service).execute(any(Runnable.class));
		sut.setExecutorService(service);

		final TimeTrackingItem item = new TimeTrackingItem("comment",
				DateTime.now());
		ItemReader reader = mock(ItemReader.class);
		given(reader.read()).willReturn(Optional.of(item),
				Optional.<TimeTrackingItem> absent());

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

		// WHEN
		sut.readHistoryFrom(reader);

		// THEN
		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				verify(service).execute(any(Runnable.class));
				assertThat(sut.history.getItems(),
						is(Arrays.asList(new TimeTrackingItem[] { item })));
			}
		});
	}

	private void givenCommand(String command) {
		TextArea commandArea = getCommandArea();
		commandArea.setText(command);
	}

	private TextArea getCommandArea() {
		return (TextArea) stage.getScene().lookup("#commandText");
	}
}
