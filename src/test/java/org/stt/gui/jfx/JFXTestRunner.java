package org.stt.gui.jfx;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Runs test within the JFX Thread.
 * 
 * @author bytekeeper
 * 
 */
public class JFXTestRunner extends BlockJUnit4ClassRunner {
	private final JFXTestHelper helper = new JFXTestHelper();

	public JFXTestRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}

	@Override
	public void run(final RunNotifier notifier) {
		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				JFXTestRunner.super.run(notifier);
			}
		});
	}
}
