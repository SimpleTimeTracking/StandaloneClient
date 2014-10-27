package org.stt.gui.jfx;

import com.sun.javafx.application.PlatformImpl;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.stage.Stage;
import javafx.stage.Window;

public class JFXTestHelper {

//	private final JFXPanel jfxPanel;
	public JFXTestHelper() {
//		try {
//			Field toolkitField = Toolkit.class.getDeclaredField("TOOLKIT");
//			toolkitField.setAccessible(true);
//			toolkitField.set(null, new DamnJFXToolkit());
//			toolkitField.setAccessible(false);
//		} catch (IllegalAccessException ex) {
//			throw new RuntimeException(ex);
//		} catch (NoSuchFieldException ex) {
//			Logger.getLogger(JFXTestHelper.class.getName()).log(Level.SEVERE, null, ex);
//		} catch (SecurityException ex) {
//			Logger.getLogger(JFXTestHelper.class.getName()).log(Level.SEVERE, null, ex);
//		}
//		System.setProperty("javafx.toolkit", DamnJFXToolkit.class.getName());
//		jfxPanel = new JFXPanel();
		PlatformImpl.startup(new Runnable() {
			@Override
			public void run() {
				// No need to do anything here
			}
		});
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
		if (Platform.isFxApplicationThread()) {
			runnable.run();
		} else {
			FutureTask<?> task = new FutureTask<>(runnable, null);
			Platform.runLater(task);
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
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
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		return stage;
	}
}
