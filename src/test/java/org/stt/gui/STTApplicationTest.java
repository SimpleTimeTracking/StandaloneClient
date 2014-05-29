package org.stt.gui;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import javafx.stage.Stage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.stt.gui.jfx.STTApplication;

// PowerMockRunner is required, because Stage uses final methods
@RunWith(PowerMockRunner.class)
@PrepareForTest(Stage.class)
public class STTApplicationTest {
	private final STTApplication sut = new STTApplication();

	@Test
	public void shouldShowWindow() throws Exception {
		// GIVEN
		Stage stage = mock(Stage.class);

		// WHEN
		sut.start(stage);

		// THEN
		verify(stage).show();
	}

	@Test
	public void shouldUseGivenTitle() throws Exception {
		// GIVEN
		Stage stage = mock(Stage.class);
		String windowTitleForTest = "windowTitleForTest";
		sut.setWindowTitle(windowTitleForTest);

		// WHEN
		sut.start(stage);

		// THEN
		verify(stage).setTitle(windowTitleForTest);
	}
}
