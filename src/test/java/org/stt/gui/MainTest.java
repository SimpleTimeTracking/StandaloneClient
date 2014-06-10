package org.stt.gui;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.stt.gui.jfx.JFXTestRunner;
import org.stt.gui.jfx.STTApplication;

@RunWith(JFXTestRunner.class)
public class MainTest extends Main {
	private final Main sut = new Main();

	@Test
	public void shouldCreateSTTApplication() {
		// GIVEN

		// WHEN
		STTApplication application = sut.createSTTApplication();

		// THEN
		assertThat(application, notNullValue());
	}

}
