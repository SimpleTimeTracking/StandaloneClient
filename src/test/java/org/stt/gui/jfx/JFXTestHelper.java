package org.stt.gui.jfx;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import javafx.stage.Window;

public class JFXTestHelper {
	public JFXTestHelper() {
		new JFXPanel();
	}

	public <T> T invokeAndWait(Callable<T> callable) {
		FutureTask<T> task = new FutureTask<>(callable);
		Platform.runLater(task);
		try {
			return task.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public void invokeAndWait(Runnable runnable) {
		FutureTask<?> task = new FutureTask<>(runnable, null);
		Platform.runLater(task);
		try {
			task.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a stage that does not open a UI window.
	 * 
	 * @return
	 */
	public Stage createStageForTest() {
		Stage stage = new Stage();
		try {
			Field showingField = Window.class.getDeclaredField("showing");
			showingField.setAccessible(true);
			showingField.set(stage, new ReadOnlyBooleanWrapper());
			showingField.setAccessible(false);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		return stage;
	}
}
