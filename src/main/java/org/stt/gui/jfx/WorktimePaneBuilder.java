package org.stt.gui.jfx;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import org.joda.time.Duration;
import org.stt.event.events.ItemModificationEvent;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.query.WorkTimeQueries;
import org.stt.time.DateTimeHelper;

import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 29.03.15.
 */
@Singleton
public class WorktimePaneBuilder implements AdditionalPaneBuilder {
    private final ResourceBundle i18n;
    private SimpleObjectProperty<Duration> remainingWorktime = new SimpleObjectProperty<>();
    private WorkTimeQueries workTimeQueries;

    @Inject
    public WorktimePaneBuilder(EventBus eventBus, ResourceBundle i18n, WorkTimeQueries workTimeQueries) {
        this.i18n = checkNotNull(i18n);
        this.workTimeQueries = checkNotNull(workTimeQueries);
        checkNotNull(eventBus).register(this);
    }

    @Subscribe
    private void updateOnModification(ItemModificationEvent event) {
        remainingWorktime.setValue(workTimeQueries.queryRemainingWorktimeToday());
    }

    @Override
    public Pane build() {
        remainingWorktime.setValue(workTimeQueries.queryRemainingWorktimeToday());
        FlowPane flowPane = new FlowPane();
        flowPane.setVgap(8);
        ObservableList<Node> elements = flowPane.getChildren();
        Label remainingWorktimeToday = new Label();
        remainingWorktimeToday.textProperty().bind(STTBindings.formattedDuration(remainingWorktime));

        elements.addAll(new Label(i18n.getString("remainingWorktimeToday")), remainingWorktimeToday);

        return flowPane;
    }
}
