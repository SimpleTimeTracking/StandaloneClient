package org.stt.gui.jfx;

import com.sun.javafx.application.PlatformImpl;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
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
	protected void runChild(final FrameworkMethod method,
			final RunNotifier notifier) {

		final PlatformImpl.FinishListener finishListener = new PlatformImpl.FinishListener() {

			@Override
			public void idle(boolean implicitExit) {
			}

			@Override
			public void exitCalled() {
			}
		};
		PlatformImpl.addListener(finishListener);

		if (method.getAnnotation(NotOnPlatformThread.class) != null) {
			super.runChild(method, notifier);
		} else {
			helper.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					JFXTestRunner.super.runChild(method, notifier);
				}
			});
		}
		PlatformImpl.removeListener(finishListener);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface NotOnPlatformThread {

	}
}
