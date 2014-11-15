package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ResourceBundle;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.util.Callback;
import org.stt.gui.jfx.TimeTrackingItemCell.ContinueActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.DeleteActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.EditActionHandler;
import org.stt.model.TimeTrackingItem;
import org.stt.model.TimeTrackingItemFilter;

public class TimeTrackingItemCellFactory implements
		Callback<ListView<TimeTrackingItem>, ListCell<TimeTrackingItem>> {
	private final Image deleteImage = new Image("/Delete.png", 25, 25, true,
			true);

	private final Image continueImage = new Image("/Continue.png", 25, 25,
			true, true);

	private final Image editImage = new Image("/Edit.png", 25, 25, true, true);

	private final Image fromToImage = new Image("/FromTo.png", 32, 12, true,
			true);

	private final Image runningImage = new Image("/Running.png", 32, 8, true,
			true);

	private final TimeTrackingItemCell.Builder timeTrackingItemCellBuilder;

	public TimeTrackingItemCellFactory(
			ContinueActionHandler continueActionHandler,
			DeleteActionHandler deleteActionHandler,
			EditActionHandler editActionHandler,
			TimeTrackingItemFilter firstItemOfTheDayFilter,
			ResourceBundle resourceBundle) {
		checkNotNull(continueActionHandler);
		checkNotNull(deleteActionHandler);
		checkNotNull(editActionHandler);
		checkNotNull(resourceBundle);

		timeTrackingItemCellBuilder = new TimeTrackingItemCell.Builder()
				.continueActionHandler(continueActionHandler)
				.deleteActionHandler(deleteActionHandler)
				.editActionHandler(editActionHandler)
				.continueImage(continueImage).deleteImage(deleteImage)
				.editImage(editImage).runningImage(runningImage)
				.fromToImage(fromToImage)
				.firstItemOfTheDayFilter(firstItemOfTheDayFilter)
				.resourceBundle(resourceBundle);
	}

	@Override
	public ListCell<TimeTrackingItem> call(ListView<TimeTrackingItem> arg0) {
		return new TimeTrackingItemCell(timeTrackingItemCellBuilder);
	}
}
