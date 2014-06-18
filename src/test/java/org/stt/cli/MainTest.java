package org.stt.cli;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;

public class MainTest {
	private Main sut;

	@Mock
	private Configuration configuration;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		sut = new Main(configuration);
	}

	@Test
	public void test() {
		fail("Not yet implemented");
	}

}
