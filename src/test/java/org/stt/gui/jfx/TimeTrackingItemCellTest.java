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
import org.stt.gui.jfx.TimeTrackingItemCell.DeleteActionHandler;
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
	@Mock
	private DeleteActionHandler deleteActionHandler;
	@Mock
	private Image imageForEdit;
	@Mock
	private Image imageForDelete;
	@Mock
	private Image imageFromTo;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		sut = new TimeTrackingItemCell(continueActionHandler,
				editActionHandler, deleteActionHandler, imageForContinue,
				imageForEdit, imageForDelete, imageFromTo);
	}

	@Test
	public void shouldUseContinueImage() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());

		// WHEN
		sut.updateItem(item, false);

		// THEN
		Pane pane = (Pane) sut.getGraphic();
		assertPanelHasImageButtonWithImage(pane, imageForContinue);
	}

	@Test
	public void shouldUseFromToImage() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());

		// WHEN
		sut.updateItem(item, false);

		// THEN
		Pane pane = (Pane) sut.getGraphic();
		assertPanelHasImageButtonWithImage(pane, imageForContinue);
	}

	@Test
	public void shouldUseEditImage() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());

		// WHEN
		sut.updateItem(item, false);

		// THEN
		Pane pane = (Pane) sut.getGraphic();
		assertPanelHasImageButtonWithImage(pane, imageForEdit);
	}

	@Test
	public void shouldUseDeleteImage() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());

		// WHEN
		sut.updateItem(item, false);

		// THEN
		Pane pane = (Pane) sut.getGraphic();
		assertPanelHasImageButtonWithImage(pane, imageForDelete);
	}

	private void assertPanelHasImageButtonWithImage(Pane pane, Image image) {
		assertThat(pane.getChildren(), hasItem(Matchers.<Node> hasProperty(
				"children",
				hasItem(Matchers.<Node> hasProperty("graphic",
						hasProperty("image", is(image)))))));
	}

	@Test
	public void shouldCallDeleteHandlerOnClickOnDelete() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());
		sut.updateItem(item, false);

		Button deleteButton = sut.deleteButton;

		// WHEN
		deleteButton.fire();

		// THEN
		verify(deleteActionHandler).delete(item);
	}

	@Test
	public void shouldCallContinueHandlerOnClickOnContinue() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());
		sut.updateItem(item, false);

		Button continueButton = sut.continueButton;

		// WHEN
		continueButton.fire();

		// THEN
		verify(continueActionHandler).continueItem(item);
	}

	@Test
	public void shouldCallEditHandlerOnClickOnEdit() {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("test", DateTime.now());
		sut.updateItem(item, false);

		Button editButton = sut.editButton;
		// WHEN
		editButton.fire();

		// THEN
		verify(editActionHandler).edit(item);
	}
}
