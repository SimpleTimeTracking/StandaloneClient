package org.stt.gui.jfx;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.wellbehaved.event.Nodes;
import org.stt.States;
import org.stt.Streams;
import org.stt.command.*;
import org.stt.config.ActivitiesConfig;
import org.stt.event.ShuttingDown;
import org.stt.fun.Achievement;
import org.stt.fun.AchievementService;
import org.stt.fun.AchievementsUpdated;
import org.stt.gui.jfx.TimeTrackingItemCell.ActionsHandler;
import org.stt.gui.jfx.binding.MappedSetBinding;
import org.stt.gui.jfx.binding.TimeTrackingListFilter;
import org.stt.gui.jfx.text.CommandHighlighter;
import org.stt.model.ItemModified;
import org.stt.model.ItemReplaced;
import org.stt.model.TimeTrackingItem;
import org.stt.query.Criteria;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.text.ExpansionProvider;
import org.stt.validation.ItemAndDateValidator;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;
import static org.stt.Strings.commonPrefix;
import static org.stt.gui.jfx.STTOptionDialogs.Result;
import static org.stt.model.TimeTrackingItems.*;

public class ActivitiesController implements ActionsHandler {

    private static final Logger LOG = Logger.getLogger(ActivitiesController.class
            .getName());
    public static final String WIKI_URL = "https://github.com/SimpleTimeTracking/StandaloneClient/wiki/CLI";
    final ObservableList<TimeTrackingItem> allItems = FXCollections
            .observableArrayList();
    private final CommandFormatter commandFormatter;
    private final Collection<ExpansionProvider> expansionProviders;
    private final ResourceBundle localization;
    private final MBassador<Object> eventBus;
    private final boolean filterDuplicatesWhenSearching;
    private final CommandHandler activities;
    private final Font fontAwesome;
    private final BorderPane panel;
    private final ActivitiesConfig activitiesConfig;
    private final ActivityTextDisplayProcessor labelToNodeMapper;
    private STTOptionDialogs sttOptionDialogs;
    private ItemAndDateValidator validator;
    private TimeTrackingItemQueries queries;
    private AchievementService achievementService;
    private ExecutorService executorService;
    StyleClassedTextArea commandText;

    private WorktimePane worktimePane;

    @FXML
    private ListView<TimeTrackingItem> activityList;
    @FXML
    private FlowPane achievements;
    @FXML
    private VBox additionals;
    @FXML
    private BorderPane commandPane;
    @FXML
    private ToolBar activityListToolbar;


    @Inject
    ActivitiesController(STTOptionDialogs sttOptionDialogs, // NOSONAR
                         MBassador<Object> eventBus,
                         CommandFormatter commandFormatter,
                         Collection<ExpansionProvider> expansionProviders,
                         ResourceBundle resourceBundle,
                         ActivitiesConfig activitiesConfig,
                         ItemAndDateValidator validator,
                         TimeTrackingItemQueries queries,
                         AchievementService achievementService,
                         ExecutorService executorService,
                         CommandHandler activities,
                         @Named("glyph") Font fontAwesome,
                         WorktimePane worktimePane,
                         @Named("activityToText") ActivityTextDisplayProcessor labelToNodeMapper) {
        this.worktimePane = requireNonNull(worktimePane);
        this.activitiesConfig = requireNonNull(activitiesConfig);
        this.executorService = requireNonNull(executorService);
        this.achievementService = requireNonNull(achievementService);
        this.queries = requireNonNull(queries);
        this.sttOptionDialogs = requireNonNull(sttOptionDialogs);
        this.validator = requireNonNull(validator);
        this.eventBus = requireNonNull(eventBus);
        this.expansionProviders = requireNonNull(expansionProviders);
        this.commandFormatter = requireNonNull(commandFormatter);
        this.localization = requireNonNull(resourceBundle);
        this.activities = requireNonNull(activities);
        this.fontAwesome = requireNonNull(fontAwesome);
        this.labelToNodeMapper = labelToNodeMapper;
        eventBus.subscribe(this);
        eventBus.subscribe(new BulkRenameHelper());
        filterDuplicatesWhenSearching = activitiesConfig.isFilterDuplicatesWhenSearching();

        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org/stt/gui/jfx/ActivitiesPanel.fxml"), localization);
        loader.setController(this);

        try {
            panel = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        panel.getStylesheets().add("org/stt/gui/jfx/Activities.css");
    }

    @Handler
    public void onAchievementsRefresh(AchievementsUpdated refreshedAchievements) {
        updateAchievements();
    }

    @Handler
    public void onItemChange(ItemModified event) {
        updateItems();
    }

    private void updateAchievements() {
        Collection<Achievement> newAchievements = achievementService.getReachedAchievements();
        Platform.runLater(() -> {
            achievements.getChildren().clear();
            for (Achievement achievement : newAchievements) {
                final String imageName = "/achievements/"
                        + achievement.getCode() + ".png";
                InputStream imageStream = getClass().getResourceAsStream(
                        imageName);
                if (imageStream != null) {
                    final ImageView imageView = new ImageView(new Image(
                            imageStream));
                    String description = achievement.getDescription();
                    if (description != null) {
                        Tooltip.install(imageView, new Tooltip(description));
                    }
                    achievements.getChildren().add(imageView);
                } else {
                    LOG.severe("Image " + imageName + " not found!");
                }
            }
        });
    }


    private void setCommandText(String textToSet) {
        setCommandText(textToSet, textToSet.length(), textToSet.length());
    }

    private void setCommandText(String textToSet, int selectionStart, int selectionEnd) {
        commandText.replaceText(textToSet);
        commandText.selectRange(selectionStart, selectionEnd);
        commandText.requestFocus();
    }

    private void insertAtCaret(String text) {
        int caretPosition = commandText.getCaretPosition();
        commandText.insertText(caretPosition, text);
        commandText.moveTo(caretPosition + text.length());
    }

    void expandCurrentCommand() {
        List<String> expansions = getSuggestedContinuations();
        if (!expansions.isEmpty()) {
            String maxExpansion = expansions.get(0);
            for (String exp : expansions) {
                maxExpansion = commonPrefix(maxExpansion, exp);
            }
            insertAtCaret(maxExpansion);
        }
    }

    private List<String> getSuggestedContinuations() {
        String textToExpand = getTextFromStartToCaret();
        return expansionProviders.stream()
                .flatMap(expansionProvider -> expansionProvider.getPossibleExpansions(textToExpand).stream())
                .collect(Collectors.toList());
    }

    private String getTextFromStartToCaret() {
        return commandText.getText(0, commandText.getCaretPosition());
    }

    void executeCommand() {
        final String text = commandText.getText();
        if (text.trim().isEmpty()) {
            return;
        }
        commandFormatter
                .parse(text)
                .accept(new ValidatingCommandHandler());
    }

    private void updateItems() {
        List<TimeTrackingItem> updateWith = queries.queryAllItems().collect(Collectors.toList());
        Platform.runLater(() -> allItems.setAll(updateWith));
    }

    @Override
    public void continueItem(TimeTrackingItem item) {
        requireNonNull(item);
        LOG.fine(() -> "Continuing item: " + item);
        activities.resumeActivity(new ResumeActivity(item, LocalDateTime.now()));
        clearCommand();

        if (activitiesConfig.isCloseOnContinue()) {
            shutdown();
        }
    }

    private void shutdown() {
        eventBus.publish(new ShuttingDown());
    }

    @Override
    public void edit(TimeTrackingItem item) {
        requireNonNull(item);
        LOG.fine(() -> "Editing item: " + item);
        setCommandText(commandFormatter.asNewItemCommandText(item), 0, item.getActivity().length());
    }

    @Override
    public void delete(TimeTrackingItem item) {
        requireNonNull(item);
        LOG.fine(() -> "Deleting item: " + item);
        if (!activitiesConfig.isAskBeforeDeleting() || sttOptionDialogs.showDeleteOrKeepDialog(item) == Result.PERFORM_ACTION) {
            RemoveActivity command = new RemoveActivity(item);
            if (activitiesConfig.isDeleteClosesGaps()) {
                activities.removeActivityAndCloseGap(command);
            } else {
                activities.removeActivity(command);
            }
        }
    }

    @Override
    public void stop(TimeTrackingItem item) {
        requireNonNull(item);
        LOG.fine(() -> "Stopping item: " + item);
        States.requireThat(!item.getEnd().isPresent(), "Item to finish is already finished");
        activities.endCurrentActivity(new EndCurrentItem(LocalDateTime.now()));
        shutdown();
    }

    @FXML
    public void initialize() {

        addWorktimePanel();
        addCommandText();
        addInsertButton();
        addNavigationButtonsForActivitiesList();

        TimeTrackingListFilter filteredList = new TimeTrackingListFilter(allItems, commandText.textProperty(),
                filterDuplicatesWhenSearching);


        ObservableSet<TimeTrackingItem> lastItemOfDay = new MappedSetBinding<>(
                () -> lastItemOf(filteredList.stream()), filteredList);

        setupCellFactory(lastItemOfDay::contains);
        final MultipleSelectionModel<TimeTrackingItem> selectionModel = activityList
                .getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);

        activityList.setItems(filteredList);
        bindItemSelection();

        executorService.execute(() -> {
            // Post initial request to load all items
            updateItems();
            updateAchievements();
        });
    }

    private void addNavigationButtonsForActivitiesList() {
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);

        FramelessButton oneWeekDownBtn = new FramelessButton(Glyph.glyph(fontAwesome, Glyph.ANGLE_DOUBLE_DOWN, 20));
        oneWeekDownBtn.setOnAction(event -> {
            VirtualFlow<?> virtualFlow = (VirtualFlow<?>) activityList.getChildrenUnmodifiable().get(0);
            IndexedCell lastVisibleCell = virtualFlow.getLastVisibleCell();
            int index = lastVisibleCell.getIndex();
            TimeTrackingItem item = (TimeTrackingItem) lastVisibleCell.getItem();
            LocalDate dateOfLastVisibleItem = item.getStart().toLocalDate();
            while (index < activityList.getItems().size()) {
                TimeTrackingItem currentItem = activityList.getItems().get(index);
                if (ChronoUnit.DAYS.between(currentItem.getStart().toLocalDate(), dateOfLastVisibleItem) >= 7) {
                    break;
                }
                index++;
            }
            activityList.scrollTo(index);
        });
        Tooltip.install(oneWeekDownBtn, new Tooltip(localization.getString("activities.list.weekDown")));
        FramelessButton oneWeekUpBtn = new FramelessButton(Glyph.glyph(fontAwesome, Glyph.ANGLE_DOUBLE_UP, 20));
        oneWeekUpBtn.setOnAction(event -> {
            VirtualFlow<?> virtualFlow = (VirtualFlow<?>) activityList.getChildrenUnmodifiable().get(0);
            IndexedCell lastVisibleCell = virtualFlow.getFirstVisibleCell();
            int index = lastVisibleCell.getIndex();
            TimeTrackingItem item = (TimeTrackingItem) lastVisibleCell.getItem();
            LocalDate dateOfLastVisibleItem = item.getStart().toLocalDate();
            while (index >= 0) {
                TimeTrackingItem currentItem = activityList.getItems().get(index);
                if (ChronoUnit.DAYS.between(dateOfLastVisibleItem, currentItem.getStart().toLocalDate()) >= 7) {
                    break;
                }
                index--;
            }
            activityList.scrollTo(index);
        });
        Tooltip.install(oneWeekUpBtn, new Tooltip(localization.getString("activities.list.weekUp")));
        activityListToolbar.getItems()
                .addAll(space,
                        oneWeekDownBtn,
                        oneWeekUpBtn);
    }

    private void addWorktimePanel() {
        additionals.getChildren().add(worktimePane);
    }

    private void addCommandText() {
        commandText = new StyleClassedTextArea();
        commandText.requestFocus();

        CommandHighlighter commandHighlighter = new CommandHighlighter(commandText);
        commandText.textProperty().addListener((observable, oldValue, newValue)
                -> commandHighlighter.update());

        commandPane.setCenter(new VirtualizedScrollPane<>(commandText));
        Tooltip.install(commandText, new Tooltip(localization.getString("activities.command.tooltip")));
        Nodes.addInputMap(commandText, sequence(
                consume(keyPressed(ENTER, CONTROL_DOWN), event -> done()),
                consume(keyPressed(SPACE, CONTROL_DOWN), event -> expandCurrentCommand()),
                consume(keyPressed(F1), event -> help())));
    }

    private void help() {
        executorService.execute(() -> {
            try {
                Desktop.getDesktop().browse(new URI(WIKI_URL));
            } catch (IOException | URISyntaxException ex) {
                LOG.log(Level.SEVERE, "Couldn't open help page", ex);
            }
        });
    }

    private void addInsertButton() {
        FramelessButton insertButton = new FramelessButton(Glyph.glyph(fontAwesome, Glyph.CHEVRON_CIRCLE_RIGHT, 60, Color.CORNFLOWERBLUE));
        insertButton.setBackground(commandText.getBackground());
        insertButton.setTooltip(new Tooltip(localization.getString("activities.command.insert")));
        insertButton.setOnAction(event -> executeCommand());
        BorderPane.setAlignment(insertButton, Pos.CENTER);
        commandPane.setRight(insertButton);
        commandPane.setBackground(commandText.getBackground());
    }

    private void bindItemSelection() {
        activityList.setOnMouseClicked(event -> {
            TimeTrackingItem selectedItem = activityList.getSelectionModel()
                    .getSelectedItem();
            resultItemSelected(selectedItem);
        });
    }

    private void resultItemSelected(TimeTrackingItem item) {
        if (item != null) {
            String textToSet = item.getActivity();
            textOfSelectedItem(textToSet);
        }
    }

    private void textOfSelectedItem(String textToSet) {
        setCommandText(textToSet);
        commandText.requestFocus();
    }

    private Set<TimeTrackingItem> lastItemOf(Stream<TimeTrackingItem> itemsToProcess) {
        return itemsToProcess.filter(Streams.distinctByKey(item -> item.getStart()
                .toLocalDate()))
                .collect(Collectors.toSet());
    }

    private void setupCellFactory(Predicate<TimeTrackingItem> lastItemOfDay) {
        activityList.setCellFactory(new TimeTrackingItemCellFactory(
                ActivitiesController.this, lastItemOfDay, localization, fontAwesome, labelToNodeMapper));
    }

    private void done() {
        executeCommand();
        shutdown();
    }

    public Node getNode() {
        return panel;
    }

    private void clearCommand() {
        commandText.clear();
    }

    private class ValidatingCommandHandler implements CommandHandler {
        @Override
        public void addNewActivity(NewActivity command) {
            TimeTrackingItem newItem = command.newItem;
            LocalDateTime start = newItem.getStart();
            if (!validateItemIsFirstItemAndLater(start) || !validateItemWouldCoverOtherItems(newItem)) {
                return;
            }
            activities.addNewActivity(command);
            clearCommand();
        }

        @Override
        public void endCurrentActivity(EndCurrentItem command) {
            activities.endCurrentActivity(command);
            clearCommand();
        }

        @Override
        public void removeActivity(RemoveActivity command) {
            activities.removeActivity(command);
            clearCommand();
        }

        @Override
        public void removeActivityAndCloseGap(RemoveActivity command) {
            activities.removeActivityAndCloseGap(command);
            clearCommand();
        }

        @Override
        public void resumeActivity(ResumeActivity command) {
            activities.resumeActivity(command);
        }

        @Override
        public void resumeLastActivity(ResumeLastActivity command) {
            activities.resumeLastActivity(command);
            clearCommand();

        }

        @Override
        public void bulkChangeActivity(Collection<TimeTrackingItem> itemsToChange, String activity) {
            activities.bulkChangeActivity(itemsToChange, activity);
        }

        private boolean validateItemIsFirstItemAndLater(LocalDateTime start) {
            return validator.validateItemIsFirstItemAndLater(start)
                    || sttOptionDialogs.showNoCurrentItemAndItemIsLaterDialog() == Result.PERFORM_ACTION;
        }

        private boolean validateItemWouldCoverOtherItems(TimeTrackingItem newItem) {
            int numberOfCoveredItems = validator.validateItemWouldCoverOtherItems(newItem);
            return numberOfCoveredItems == 0 || sttOptionDialogs.showItemCoversOtherItemsDialog(numberOfCoveredItems) == Result.PERFORM_ACTION;
        }
    }

    @Listener(references = References.Strong)
    private class BulkRenameHelper {
        private boolean updating;

        @Handler
        public void onItemReplaced(ItemReplaced event) {
            if (updating) {
                return;
            }
            updating = true;
            try {
                TimeTrackingItem beforeUpdate = event.beforeUpdate;
                TimeTrackingItem afterUpdate = event.afterUpdate;
                if (!sameStart(beforeUpdate, afterUpdate)
                        || sameActivity(beforeUpdate, afterUpdate)
                        || !sameEndOrWasOngoing(beforeUpdate, afterUpdate)) {
                    return;
                }
                Criteria criteria = new Criteria().withActivityIs(beforeUpdate.getActivity());
                List<TimeTrackingItem> activityItems = queries.queryItems(criteria).collect(Collectors.toList());
                if (activityItems.isEmpty()) {
                    return;
                }
                Result renameResult = sttOptionDialogs.showRenameDialog(activityItems.size(), beforeUpdate.getActivity(), afterUpdate.getActivity());
                if (Result.PERFORM_ACTION == renameResult) {
                    activities.bulkChangeActivity(activityItems, event.afterUpdate.getActivity());
                }
            } finally {
                updating = false;
            }
        }

        private boolean sameEndOrWasOngoing(TimeTrackingItem beforeUpdate, TimeTrackingItem afterUpdate) {
            return sameEnd(beforeUpdate, afterUpdate)
                    || !beforeUpdate.getEnd().isPresent();
        }
    }
}
