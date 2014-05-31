package org.stt.gui;

import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.stt.gui.jfx.STTApplication;

// PowerMockRunner is required, because Stage uses final methods
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Stage.class, Scene.class })
public class STTApplicationTest {
	private final STTApplication sut = new STTApplication();
	private final JFXTestHelper helper = new JFXTestHelper();

	@Test
	public void shouldShowWindow() throws Exception {
		// GIVEN
		final Stage stage = mock(Stage.class);

		// WHEN
		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				try {
					sut.start(stage);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// THEN
		verify(stage).show();
	}
}
