package org.stt.gui.jfx;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.stt.CommandHandler;

@RunWith(JFXTestRunner.class)
public class STTApplicationTest {
	private final STTApplication sut = new STTApplication();
	private final JFXTestHelper helper = new JFXTestHelper();
	private Stage stage;
	private CommandHandler commandHandler;

	@Before
	public void setup() {
		stage = helper.createStageForTest();
		sut.setStage(stage);
		commandHandler = mock(CommandHandler.class);
		sut.setCommandHandler(commandHandler);
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

	private void givenCommand(String command) {
		TextArea commandArea = getCommandArea();
		commandArea.setText(command);
	}

	private TextArea getCommandArea() {
		return (TextArea) stage.getScene().lookup("#commandText");
	}
}
