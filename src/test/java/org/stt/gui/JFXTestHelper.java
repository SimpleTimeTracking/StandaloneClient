package org.stt.gui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

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
}
