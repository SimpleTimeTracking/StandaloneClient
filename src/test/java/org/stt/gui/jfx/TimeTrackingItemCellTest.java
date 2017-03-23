package org.stt.gui.jfx;

import javafx.scene.control.Button;
import javafx.scene.text.Font;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.gui.jfx.TimeTrackingItemCell.ActionsHandler;
import org.stt.model.TimeTrackingItem;

import java.time.LocalDateTime;
import java.util.ResourceBundle;

import static org.mockito.Mockito.verify;

public class TimeTrackingItemCellTest {

	private TimeTrackingItemCell sut;
	@Mock
    private ActionsHandler actionsHandler;
    private Font fontAwesome;

    @Before
    public void setup() throws Throwable {
        TestFX.installTK();
        fontAwesome = Font.loadFont(getClass().getResourceAsStream("/fontawesome-webfont.ttf"), 0);
        MockitoAnnotations.initMocks(this);
		ResourceBundle resourceBundle = ResourceBundle
				.getBundle("org.stt.gui.Application");

        sut = new TimeTrackingItemCell(fontAwesome, resourceBundle, a -> false, actionsHandler) {

			@Override
			protected void setupTooltips(ResourceBundle localization) {
			}
		};
	}

	@Test
	public void shouldCallDeleteHandlerOnClickOnDelete() {
		// GIVEN
        TimeTrackingItem item = new TimeTrackingItem("test", LocalDateTime.now());
        sut.updateItem(item, false);

		Button deleteButton = sut.deleteButton;

		// WHEN
		deleteButton.fire();

		// THEN
        verify(actionsHandler).delete(item);
    }

	@Test
	public void shouldCallContinueHandlerOnClickOnContinue() {
		// GIVEN
        TimeTrackingItem item = new TimeTrackingItem("test", LocalDateTime.now());
        sut.updateItem(item, false);

		Button continueButton = sut.continueButton;

		// WHEN
		continueButton.fire();

		// THEN
        verify(actionsHandler).continueItem(item);
    }

	@Test
	public void shouldCallEditHandlerOnClickOnEdit() {
		// GIVEN
        TimeTrackingItem item = new TimeTrackingItem("test", LocalDateTime.now());
        sut.updateItem(item, false);

		Button editButton = sut.editButton;
		// WHEN
		editButton.fire();

		// THEN
        verify(actionsHandler).edit(item);
    }

    @Test
    public void shouldCallStopHandlerOnClickOnEdit() {
        // GIVEN
        TimeTrackingItem item = new TimeTrackingItem("test", LocalDateTime.now());
        sut.updateItem(item, false);

        Button stopButton = sut.stopButton;
        // WHEN
        stopButton.fire();

        // THEN
        verify(actionsHandler).stop(item);
    }
}
