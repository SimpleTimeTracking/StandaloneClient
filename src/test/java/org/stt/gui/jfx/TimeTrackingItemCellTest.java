package org.stt.gui.jfx;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.gui.jfx.TimeTrackingItemCell.ContinueActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.EditActionHandler;
import org.stt.model.TimeTrackingItem;

public class TimeTrackingItemCellTest {
	private TimeTrackingItemCell sut;
	@Mock
	private ContinueActionHandler continueActionHandler;
	@Mock
	private Image imageForContinue;
	@Mock
	private EditActionHandler editActionHandler;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		sut = new TimeTrackingItemCell(continueActionHandler,
				editActionHandler, imageForContinue, "edit");
	}

	@Test
	public void shouldUseGivenImage() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());

		// WHEN
		sut.updateItem(item, false);

		// THEN
		Pane pane = (Pane) sut.getGraphic();
		assertThat(
				pane.getChildren(),
				hasItem(Matchers.<Node> hasProperty("graphic",
						hasProperty("image", is(imageForContinue)))));
	}

	@Test
	public void shouldCallContinueHandlerOnClickOnContinue() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());
		sut.updateItem(item, false);

		Pane pane = (Pane) sut.getGraphic();
		ImageButton btn = (ImageButton) pane.lookup("ImageButton");

		// WHEN
		btn.fire();

		// THEN
		verify(continueActionHandler).continueItem(item);
	}

	@Test
	public void shouldCallEditHandlerOnClickOnEdit() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());
		sut.updateItem(item, false);

		Pane pane = (Pane) sut.getGraphic();
		Button editButton = (Button) pane.lookup("Button");
		// WHEN
		editButton.fire();

		// THEN
		verify(editActionHandler).edit(item);
	}
}
