package org.stt.gui.jfx;

import java.io.IOException;

import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Factory;
import org.stt.persistence.ItemReaderProvider;
import org.stt.searching.ItemSearcher;

@RunWith(JFXTestRunner.class)
public class ReportWindowBuilderTest {
	private ReportWindowBuilder sut;
	private final JFXTestHelper helper = new JFXTestHelper();

	@Mock
	private ItemReaderProvider readerProvider;
	@Mock
	private ItemSearcher searcher;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				Factory<Stage> stageFactory = new Factory<Stage>() {

					@Override
					public Stage create() {
						return helper.createStageForTest();
					}

				};
				sut = new ReportWindowBuilder(stageFactory, readerProvider,
						searcher);
			}
		});
	}

	@Test
	public void shouldBeAbleToSetupStage() throws IOException {
		// GIVEN

		// WHEN
		sut.setupStage();

		// THEN
	}

}
