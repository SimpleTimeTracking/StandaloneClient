package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class ImageButton extends Button {
	private static final String STYLE_HOVER = "-fx-background-color: transparent; -fx-padding: 5;  -fx-effect: innershadow(gaussian, rgba(128, 255, 220, 0.5), 10, 0.5, 2, 2);";
	private static final String STYLE_NORMAL = "-fx-background-color: transparent; -fx-padding: 5; -fx-effect: null";
	private static final String STYLE_PRESSED = "-fx-background-color: transparent; -fx-padding: 7 4 3 6;  -fx-effect: null";

	public ImageButton(Image image) {
		checkNotNull(image);
		setGraphic(new ImageView(image));
		setStyle(STYLE_NORMAL);

		setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				setStyle(STYLE_PRESSED);
			}
		});

		EventHandler<MouseEvent> setStyleToNormalEventHandler = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				setStyle(STYLE_NORMAL);
			}
		};
		setOnMouseReleased(setStyleToNormalEventHandler);

		setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				setStyle(STYLE_HOVER);
			}
		});
		setOnMouseExited(setStyleToNormalEventHandler);
	}
}
