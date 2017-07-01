package org.stt.gui.jfx;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.Font;
import javafx.util.Callback;
import org.stt.gui.jfx.TimeTrackingItemCellWithActions.ActionsHandler;
import org.stt.model.TimeTrackingItem;

import java.util.ResourceBundle;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

class TimeTrackingItemCellFactory implements
		Callback<ListView<TimeTrackingItem>, ListCell<TimeTrackingItem>> {

    private final ActionsHandler actionsHandler;
    private final Predicate<TimeTrackingItem> lastItemOfDay;
    private final ResourceBundle resourceBundle;
    private final Font fontAwesome;
    private final ActivityTextDisplayProcessor labelToNodeMapper;

    public TimeTrackingItemCellFactory(
            ActionsHandler actionsHandler,
            Predicate<TimeTrackingItem> lastItemOfDay,
            ResourceBundle localization,
            Font fontAwesome,
            ActivityTextDisplayProcessor labelToNodeMapper) {
        this.actionsHandler = requireNonNull(actionsHandler);
        this.lastItemOfDay = requireNonNull(lastItemOfDay);
        this.resourceBundle = requireNonNull(localization);
        this.fontAwesome = requireNonNull(fontAwesome);
        this.labelToNodeMapper = labelToNodeMapper;
    }

	@Override
	public ListCell<TimeTrackingItem> call(ListView<TimeTrackingItem> arg0) {
        return new TimeTrackingItemCellWithActions(fontAwesome, resourceBundle, lastItemOfDay, actionsHandler, labelToNodeMapper);
    }
}
