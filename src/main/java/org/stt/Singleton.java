package org.stt;

import static com.google.common.base.Preconditions.checkState;

public abstract class Singleton<T> implements Factory<T> {
	private T instance;

	@Override
	public T create() {
		if (instance == null) {
			instance = createInstance();
			checkState(instance != null);
		}
		return instance;
	}

	protected abstract T createInstance();
}
