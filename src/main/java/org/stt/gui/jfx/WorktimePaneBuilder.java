package org.stt.gui.jfx;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import net.engio.mbassy.listener.Handler;
import org.stt.event.TimePassedEvent;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.model.ItemModified;
import org.stt.query.WorkTimeQueries;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;

@Singleton
public class WorktimePaneBuilder implements AdditionalPaneBuilder {
    private static final Logger LOG = Logger.getLogger(WorktimePaneBuilder.class.getName());
    private final ResourceBundle i18n;
    private SimpleObjectProperty<Duration> remainingWorktime = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Duration> weekWorktime = new SimpleObjectProperty<>();
    private WorkTimeQueries workTimeQueries;

    @Inject
    public WorktimePaneBuilder(ResourceBundle i18n, WorkTimeQueries workTimeQueries) {
        this.i18n = Objects.requireNonNull(i18n);
        this.workTimeQueries = Objects.requireNonNull(workTimeQueries);
    }

    @Handler
    public void updateOnModification(ItemModified event) {
        updateItems();
    }

    @Handler
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

        elements.add(hbox(4, new Label(i18n.getString("remainingWorktimeToday")), remainingWorktimeToday));
        elements.add(hbox(4, new Label(i18n.getString("weekWorktime")), weekWorktimeLabel));

        return flowPane;
    }

    private HBox hbox(double spacing, Node... nodes) {
        HBox hBox = new HBox(spacing);
        hBox.getChildren().addAll(nodes);
        return hBox;
    }
}
