package org.stt.persistence;

public interface ItemReaderProvider {
	/**
	 * Provides a new ItemReader which starts reading from the beginning.
	 **/
	ItemReader provideReader();
}
