package org.stt;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

public class ConfigurationTest {
	private final Configuration sut = new Configuration();

	@Test
	public void shouldBeAbleToProvideSTTFile() {
		// GIVEN

		// WHEN
		File sttFile = sut.getSttFile();

		// THEN
		assertThat(sttFile, notNullValue());
	}

}
