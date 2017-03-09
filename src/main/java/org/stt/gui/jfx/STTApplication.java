package org.stt.gui.jfx;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.stage.Stage;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.stt.States;
import org.stt.Streams;
import org.stt.command.*;
import org.stt.config.CommandTextConfig;
import org.stt.config.TimeTrackingItemListConfig;
import org.stt.event.ShuttingDown;
import org.stt.fun.Achievement;
import org.stt.fun.AchievementService;
import org.stt.fun.AchievementsUpdated;
import org.stt.gui.jfx.TimeTrackingItemCell.ActionsHandler;
import org.stt.gui.jfx.binding.MappedListBinding;
import org.stt.gui.jfx.binding.MappedSetBinding;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.gui.jfx.binding.TimeTrackingListFilter;
import org.stt.gui.jfx.text.CommandHighlighter;
import org.stt.gui.jfx.text.ContextPopupCreator;
import org.stt.model.ItemModified;
import org.stt.model.TimeTrackingItem;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.stt.Strings.commonPrefix;
import static org.stt.gui.jfx.STTOptionDialogs.Result;

public class STTApplication implements ActionsHandler {

    private static final Logger LOG = Logger.getLogger(STTApplication.class
            .getName());
    final ObservableList<TimeTrackingItem> allItems = FXCollections
            .observableArrayList();
    final StringProperty currentCommand = new SimpleStringProperty("");
    final IntegerProperty commandCaretPosition = new SimpleIntegerProperty();
    private final CommandFormatter commandFormatter;
    private final ReportWindowBuilder reportWindowBuilder;
    private final ExpansionProvider expansionProvider;
    private final ResourceBundle localization;
    private final MBassador<Object> eventBus;
    private final boolean autoCompletionPopup;
    private final boolean askBeforeDeleting;
    private final CommandHandler activities;
    private final Font fontAwesome;
    final ObservableList<TimeTrackingItem> filteredList;
    ViewAdapter viewAdapter;
    private STTOptionDialogs sttOptionDialogs;
    private ItemAndDateValidator validator;
    private TimeTrackingItemQueries searcher;
    private AchievementService achievementService;
    private ExecutorService executorService;
    private ObservableList<AdditionalPaneBuilder> additionals = FXCollections.observableArrayList();

    @Inject
    STTApplication(STTOptionDialogs sttOptionDialogs,
                   MBassador<Object> eventBus,
                   CommandFormatter commandFormatter,
                   ReportWindowBuilder reportWindowBuilder,
                   ExpansionProvider expansionProvider,
                   ResourceBundle resourceBundle,
                   TimeTrackingItemListConfig timeTrackingItemListConfig,
                   CommandTextConfig commandTextConfig,
                   ItemAndDateValidator validator,
                   TimeTrackingItemQueries searcher,
                   AchievementService achievementService,
                   ExecutorService executorService,
                   CommandHandler activities,
                   @Named("glyph") Font fontAwesome) {
        requireNonNull(timeTrackingItemListConfig);
        this.executorService = requireNonNull(executorService);
        this.achievementService = requireNonNull(achievementService);
        this.searcher = requireNonNull(searcher);
        this.sttOptionDialogs = requireNonNull(sttOptionDialogs);
        this.validator = requireNonNull(validator);
        this.eventBus = requireNonNull(eventBus);
        this.expansionProvider = requireNonNull(expansionProvider);
        this.reportWindowBuilder = requireNonNull(reportWindowBuilder);
        this.commandFormatter = requireNonNull(commandFormatter);
        this.localization = requireNonNull(resourceBundle);
        this.activities = requireNonNull(activities);
        this.fontAwesome = requireNonNull(fontAwesome);
        autoCompletionPopup = requireNonNull(commandTextConfig).isAutoCompletionPopup();

        eventBus.subscribe(this);
        filteredList = new TimeTrackingListFilter(allItems, currentCommand,
                timeTrackingItemListConfig.isFilterDuplicatesWhenSearching());
        askBeforeDeleting = timeTrackingItemListConfig.isAskBeforeDeleting();
    }

    @Handler
    public void onAchievementsRefresh(AchievementsUpdated refreshedAchievements) {
        updateAchievements();
    }

    @Handler(priority = -1)
    public void onItemChange(ItemModified event) {
        updateItems();
    }

    private void updateAchievements() {
        viewAdapter.updateAchievements(achievementService.getReachedAchievements());
    }


    private void setCommandText(String textToSet) {
        setCommandText(textToSet, textToSet.length());
    }

    private void setCommandText(String textToSet, int caretPosition) {
        currentCommand.set(textToSet);
        commandCaretPosition.set(caretPosition);
        viewAdapter.requestFocusOnCommandText();
    }

    private void insertAtCaret(String text) {
        int caretPosition = commandCaretPosition.get();
        String currentText = currentCommand.get();
        String prefix = getTextFromStartToCaret() + text;
        currentCommand.setValue(prefix + currentText.substring(caretPosition));
        commandCaretPosition.set(prefix.length());
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
        return expansionProvider
                .getPossibleExpansions(textToExpand);
    }

    private String getTextFromStartToCaret() {
        int caretPosition = commandCaretPosition.get();
        String currentCommandText = currentCommand.get();
        return currentCommandText.substring(0, Math.min(caretPosition, currentCommandText.length()));
    }

    void executeCommand() {
        final String text = currentCommand.get();
        if (text.trim().isEmpty()) {
            return;
        }
        commandFormatter
                .parse(text)
                .accept(new ValidatingCommandHandler());
    }

    private void show(Stage primaryStage) {
        viewAdapter = new ViewAdapter(primaryStage);
        viewAdapter.show();
    }

    public void start(Stage primaryStage) {
        show(primaryStage);
        executorService.execute(() -> {
            // Post initial request to load all items
            updateItems();
            updateAchievements();
        });
    }

    private void updateItems() {
        viewAdapter.updateAllItems(searcher.queryAllItems().collect(Collectors.toList()));
    }

    @Override
    public void continueItem(TimeTrackingItem item) {
        LOG.fine(() -> "Continuing item: " + item);
        activities.resumeActivity(new ResumeActivity(item, LocalDateTime.now()));
        viewAdapter.shutdown();
    }

    @Override
    public void edit(TimeTrackingItem item) {
        setCommandText(commandFormatter.asNewItemCommandText(item), item.getActivity().length());
    }

    @Override
    public void delete(TimeTrackingItem item) {
        requireNonNull(item);
        LOG.fine(() -> "Deleting item: " + item);
        if (!askBeforeDeleting || sttOptionDialogs.showDeleteOrKeepDialog(viewAdapter.stage, item) == Result.PERFORM_ACTION) {
            activities.removeActivity(new RemoveActivity(item));
            allItems.remove(item);
        }
    }

    @Override
    public void stop(TimeTrackingItem item) {
        requireNonNull(item);
        LOG.fine(() -> "Stopping item: " + item);
        States.requireThat(!item.getEnd().isPresent(), "Item to finish is already finished");
        activities.endCurrentActivity(new EndCurrentItem(LocalDateTime.now()));
        viewAdapter.shutdown();
    }

    public void addAdditional(AdditionalPaneBuilder builder) {
        additionals.add(builder);
    }

    public class ViewAdapter {

        final Stage stage;

        StyleClassedTextArea commandText;

        @FXML
        ListView<TimeTrackingItem> activityList;

        @FXML
        FlowPane achievements;

        @FXML
        VBox additionals;

        @FXML
        BorderPane commandPane;


        ViewAdapter(Stage stage) {
            this.stage = stage;
        }

        protected void show() {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/stt/gui/jfx/ActivitiesPanel.fxml"), localization);
            loader.setController(this);

            BorderPane pane;
            try {
                pane = loader.load();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            ObservableList<Node> additionalPanels = additionals.getChildren();
            for (AdditionalPaneBuilder builder : STTApplication.this.additionals) {
                additionalPanels.add(builder.build());
            }
            STTApplication.this.additionals.clear();

            Scene scene = new Scene(pane);
            scene.getStylesheets().add("org/stt/gui/jfx/CommandText.css");

            stage.setScene(scene);
            stage.setTitle(localization.getString("window.title"));
            Image applicationIcon = new Image("/Logo.png", 32, 32, true, true);
            stage.getIcons().add(applicationIcon);

            stage.setOnCloseRequest(event -> Platform.runLater(this::shutdown));
            scene.setOnKeyPressed(event -> {
                if (KeyCode.ESCAPE.equals(event.getCode())) {
                    event.consume();
                    shutdown();
                }
            });

            stage.show();
            requestFocusOnCommandText();

            CommandHighlighter commandHighlighter = new CommandHighlighter(commandText);
            commandText.textProperty().addListener((observable, oldValue, newValue)
                    -> commandHighlighter.addHighlights(currentCommand.get()));

            if (autoCompletionPopup) {
                setupAutoCompletionPopup();
            }
        }

        private void setupAutoCompletionPopup() {
            ObservableList<String> suggestionsForContinuationList = createSuggestionsForContinuationList();
            ListView<String> contentOfAutocompletionPopup = new ListView<>(suggestionsForContinuationList);
            final Popup popup = ContextPopupCreator.createPopupForContextMenu(contentOfAutocompletionPopup, item -> insertAtCaret(item.endsWith(" ") ? item : item + " "));
            suggestionsForContinuationList.addListener((ListChangeListener<String>) c -> {
                if (c.getList().isEmpty()) {
                    popup.hide();
                } else {
                    popup.show(stage);
                }
            });
            popup.show(stage);
        }

        private ObservableList<String> createSuggestionsForContinuationList() {
            return new MappedListBinding<>(() -> {
                List<String> suggestedContinuations = getSuggestedContinuations();
                Collections.sort(suggestedContinuations);
                return suggestedContinuations;
            }, commandCaretPosition, currentCommand);
        }

        void updateAchievements(final Collection<Achievement> newAchievements) {
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

        protected void shutdown() {
            try {
                stage.close();
            } finally {
                eventBus.publish(new ShuttingDown());
            }
        }

        @FXML
        public void initialize() {
            addCommandText();
            addInsertButton();


            setupCellFactory();
            final MultipleSelectionModel<TimeTrackingItem> selectionModel = activityList
                    .getSelectionModel();
            selectionModel.setSelectionMode(SelectionMode.SINGLE);

            STTBindings.bidirectionBindCaretPosition(commandText, commandCaretPosition);
            STTBindings.bidirectionBindTextArea(commandText, currentCommand);

            activityList.setItems(filteredList);
            bindItemSelection();
        }

        private void addCommandText() {
            commandText = new StyleClassedTextArea();
            commandPane.setCenter(new VirtualizedScrollPane<>(commandText));
            Tooltip.install(commandText, new Tooltip(localization.getString("activities.command.tooltip")));
        }

        private void addInsertButton() {
            FramelessButton insertButton = new FramelessButton(Glyph.glyph(fontAwesome, Glyph.CHEVRON_CIRCLE_RIGHT, 50));
            insertButton.setBackground(commandText.getBackground());
            insertButton.setAlignment(Pos.CENTER_LEFT);
            insertButton.setTooltip(new Tooltip(localization.getString("activities.command.insert")));
            insertButton.setOnAction(event -> executeCommand());
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
            viewAdapter.requestFocusOnCommandText();
        }

        private Set<TimeTrackingItem> lastItemOf(Stream<TimeTrackingItem> itemsToProcess) {
            return itemsToProcess.filter(Streams.distinctByKey(item -> item.getStart()
                    .toLocalDate()))
                    .collect(Collectors.toSet());
        }

        private void setupCellFactory() {
            ObservableSet<TimeTrackingItem> lastItemOfDay = new MappedSetBinding<>(
                    () -> lastItemOf(filteredList.stream()), filteredList);

            activityList.setCellFactory(new TimeTrackingItemCellFactory(
                    STTApplication.this, lastItemOfDay::contains, localization, fontAwesome));
        }

        protected void requestFocusOnCommandText() {
            Platform.runLater(commandText::requestFocus);
        }

        protected void updateAllItems(
                final Collection<TimeTrackingItem> updateWith) {
            Platform.runLater(() -> {
                allItems.setAll(updateWith);
                viewAdapter.requestFocusOnCommandText();
            });
        }

        @FXML
        void showReportWindow() {
            reportWindowBuilder.setupStage();
        }

        @FXML
        private void done() {
            executeCommand();
            shutdown();
        }

        @FXML
        private void onKeyPressed(KeyEvent event) {
            if (KeyCode.ENTER.equals(event.getCode()) && event.isControlDown()) {
                event.consume();
                done();
            }
            if (KeyCode.SPACE.equals(event.getCode()) && event.isControlDown()) {
                expandCurrentCommand();
                event.consume();
            }
            if (KeyCode.F1.equals(event.getCode())) {
                try {
                    Desktop.getDesktop()
                            .browse(new URI(
                                    "https://github.com/Bytekeeper/STT/wiki/CLI"));
                } catch (IOException | URISyntaxException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    private class ValidatingCommandHandler implements CommandHandler {
        @Override
        public void addNewActivity(NewItemCommand command) {
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
        public void resumeActivity(ResumeActivity command) {
            activities.resumeActivity(command);
            clearCommand();
        }

        private boolean validateItemIsFirstItemAndLater(LocalDateTime start) {
            return validator.validateItemIsFirstItemAndLater(start)
                    || sttOptionDialogs.showNoCurrentItemAndItemIsLaterDialog(viewAdapter.stage) == Result.PERFORM_ACTION;
        }

        private boolean validateItemWouldCoverOtherItems(TimeTrackingItem newItem) {
            int numberOfCoveredItems = validator.validateItemWouldCoverOtherItems(newItem);
            return numberOfCoveredItems == 0 || sttOptionDialogs.showItemCoversOtherItemsDialog(viewAdapter.stage, numberOfCoveredItems) == Result.PERFORM_ACTION;
        }

        private void clearCommand() {
            currentCommand.set("");
        }
    }
}
