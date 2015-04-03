package org.stt.gui.jfx;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.joda.time.Duration;
import org.stt.event.TimePassedEvent;
import org.stt.event.events.ItemModificationEvent;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.query.WorkTimeQueries;
import org.stt.time.DateTimeHelper;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 29.03.15.
 */
@Singleton
public class WorktimePaneBuilder implements AdditionalPaneBuilder {
    private static final Logger LOG = Logger.getLogger(WorktimePaneBuilder.class.getName());
    private final ResourceBundle i18n;
    private SimpleObjectProperty<Duration> remainingWorktime = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Duration> weekWorktime = new SimpleObjectProperty<>();
    private WorkTimeQueries workTimeQueries;

    @Inject
    public WorktimePaneBuilder(ResourceBundle i18n, WorkTimeQueries workTimeQueries) {
        this.i18n = checkNotNull(i18n);
        this.workTimeQueries = checkNotNull(workTimeQueries);
    }

    @Subscribe
    public void updateOnModification(ItemModificationEvent event) {
        updateItems();
    }

    @Subscribe
    public void timePassed(TimePassedEvent event) {
        updateItems();
    }

    private void updateItems() {
        LOG.finest("Updating remaining worktime");
        remainingWorktime.setValue(workTimeQueries.queryRemainingWorktimeToday());
        weekWorktime.setValue(workTimeQueries.queryWeekWorktime());
    }

    @Override
    public Pane build() {
        updateItems();
        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(20);
        ObservableList<Node> elements = flowPane.getChildren();
        Label remainingWorktimeToday = new Label();
        remainingWorktimeToday.textProperty().bind(STTBindings.formattedDuration(remainingWorktime));
        Label weekWorktimeLabel = new Label();
        weekWorktimeLabel.textProperty().bind(STTBindings.formattedDuration(weekWorktime));

        elements.add(new HBox(4, new Label(i18n.getString("remainingWorktimeToday")), remainingWorktimeToday));
        elements.add(new HBox(4, new Label(i18n.getString("weekWorktime")), weekWorktimeLabel));

        return flowPane;
    }
}
