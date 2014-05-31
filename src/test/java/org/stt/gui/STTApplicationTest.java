package org.stt.gui;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javafx.stage.Stage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.stt.gui.jfx.STTApplication;

@RunWith(JFXTestRunner.class)
public class STTApplicationTest {
	private final STTApplication sut = new STTApplication();

	@Test
	public void shouldShowWindow() throws Exception {

		// GIVEN
		final Stage stage = new Stage();

		// WHEN
		try {
			sut.start(stage);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		// THEN
		assertThat(stage.isShowing(), is(true));

	}
}
