package org.stt.gui.jfx;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.joda.time.DateTime;
import org.stt.analysis.ExpansionProvider;
import org.stt.command.Command;
import org.stt.command.CommandParser;
import org.stt.command.NewItemCommand;
import org.stt.command.NothingCommand;
import org.stt.config.CommandTextConfig;
import org.stt.config.TimeTrackingItemListConfig;
import org.stt.event.ShutdownRequest;
import org.stt.event.messages.ReadItemsResult;
import org.stt.event.messages.ReadItemsRequest;
import org.stt.event.messages.RefreshedAchievements;
import org.stt.fun.Achievement;
import org.stt.g4.EnglishCommandsLexer;
import org.stt.g4.EnglishCommandsParser;
import org.stt.gui.jfx.TimeTrackingItemCell.ContinueActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.DeleteActionHandler;
import org.stt.gui.jfx.TimeTrackingItemCell.EditActionHandler;
import org.stt.gui.jfx.binding.FirstItemOfDaySet;
import org.stt.gui.jfx.binding.TimeTrackingListFilter;
import org.stt.gui.jfx.text.CommandHighlighter;
import org.stt.gui.jfx.text.ContextPopupCreator;
import org.stt.gui.jfx.text.HighlightingOverlay;
import org.stt.gui.jfx.text.PopupAtCaretPlacer;
import org.stt.model.TimeTrackingItem;
import org.stt.model.TimeTrackingItemFilter;
import org.stt.validation.ItemAndDateValidator;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.stt.gui.jfx.STTOptionDialogs.Result;

public class STTApplication implements DeleteActionHandler, EditActionHandler,
        ContinueActionHandler {

    private static final Logger LOG = Logger.getLogger(STTApplication.class
            .getName());
    final ObservableList<TimeTrackingItem> allItems = FXCollections
            .observableArrayList();
    final StringProperty currentCommand = new SimpleStringProperty("");
    final IntegerProperty commandCaretPosition = new SimpleIntegerProperty();
    private final CommandParser commandParser;
    private final ReportWindowBuilder reportWindowBuilder;
    private final ExpansionProvider expansionProvider;
    private final ResourceBundle localization;
    private final EventBus eventBus;
    private final boolean autoCompletionPopup;
    private final boolean askBeforeDeleting;
    ObservableList<TimeTrackingItem> filteredList;
    ViewAdapter viewAdapter;
    private STTOptionDialogs sttOptionDialogs;
    private ItemAndDateValidator validator;

    @Inject
    STTApplication(STTOptionDialogs STTOptionDialogs,
                   EventBus eventBus,
                   CommandParser commandParser,
                   ReportWindowBuilder reportWindowBuilder,
                   ExpansionProvider expansionProvider,
                   ResourceBundle resourceBundle,
                   TimeTrackingItemListConfig timeTrackingItemListConfig,
                   CommandTextConfig commandTextConfig,
                   ItemAndDateValidator validator) {
        this.sttOptionDialogs = checkNotNull(STTOptionDialogs);
        this.validator = checkNotNull(validator);
        this.eventBus = checkNotNull(eventBus);
        eventBus.register(this);
        this.expansionProvider = checkNotNull(expansionProvider);
        this.reportWindowBuilder = checkNotNull(reportWindowBuilder);
        this.commandParser = checkNotNull(commandParser);
        this.localization = checkNotNull(resourceBundle);
        checkNotNull(timeTrackingItemListConfig);
        autoCompletionPopup = checkNotNull(commandTextConfig).isAutoCompletionPopup();

        filteredList = new TimeTrackingListFilter(allItems, currentCommand,
                timeTrackingItemListConfig.isFilterDuplicatesWhenSearching());
        askBeforeDeleting = timeTrackingItemListConfig.isAskBeforeDeleting();
    }

    @Subscribe
    public void receiveItems(ReadItemsResult event) {
        LOG.info("Received item event " + event);
        viewAdapter.updateAllItems(event.timeTrackingItems);
    }

    @Subscribe
    public void onAchievementsRefresh(RefreshedAchievements refreshedAchievements) {
        viewAdapter.updateAchievements(refreshedAchievements.reachedAchievements);
    }


    protected void resultItemSelected(TimeTrackingItem item) {
        if (item != null && item.getComment().isPresent()) {
            String textToSet = item.getComment().get();
            textOfSelectedItem(textToSet);
        }
    }

    public void textOfSelectedItem(String textToSet) {
        setCommandText(textToSet);
        viewAdapter.requestFocusOnCommandText();
    }

    private void setCommandText(String textToSet) {
        setCommandText(textToSet, textToSet.length());
    }

    private void setCommandText(String textToSet, int caretPosition) {
        currentCommand.set(textToSet);
        commandCaretPosition.set(caretPosition);
    }

    protected void insertAtCaret(String text) {
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

    String commonPrefix(String a, String b) {
        for (int i = 0; i < a.length() && i < b.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a;
    }

    protected boolean executeCommand() {
        final String text = currentCommand.get();
        if (!text.trim().isEmpty()) {
            Command command = commandParser
                    .parseCommandString(text).or(NothingCommand.INSTANCE);
            if (command instanceof NewItemCommand) {
                TimeTrackingItem newItem = ((NewItemCommand) command).newItem;
                DateTime start = newItem.getStart();
                if (!validateItemIsFirstItemAndLater(start) || !validateItemWouldCoverOtherItems(newItem)) {
                    command = NothingCommand.INSTANCE;
                }
            }
            if (!NothingCommand.INSTANCE.equals(command)) {
                command.execute();
                clearCommand();
                return true;
            }
        }
        return false;
    }

    private boolean validateItemIsFirstItemAndLater(DateTime start) {
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

    public void show(Stage primaryStage) {
        viewAdapter = new ViewAdapter(primaryStage);
        viewAdapter.show();
    }

    public void start(Stage primaryStage) {
        show(primaryStage);
        // Post initial request to load all items
        eventBus.post(new ReadItemsRequest());
    }

    @Override
    public void continueItem(TimeTrackingItem item) {
        commandParser.resumeItemCommand(item).execute();
        viewAdapter.shutdown();
    }

    @Override
    public void edit(TimeTrackingItem item) {
        setCommandText(commandParser.itemToCommand(item), item.getComment().or("").length());
    }

    @Override
    public void delete(TimeTrackingItem item) {
        checkNotNull(item);
        if (!askBeforeDeleting || sttOptionDialogs.showDeleteOrKeepDialog(viewAdapter.stage, item) == Result.PERFORM_ACTION) {
            commandParser.deleteCommandFor(item).execute();
            allItems.remove(item);
        }
    }

    public class ViewAdapter {

        final Stage stage;

        @FXML
        TextArea commandText;

        @FXML
        Button finButton;

        @FXML
        Button insertButton;

        @FXML
        ListView<TimeTrackingItem> result;

        @FXML
        FlowPane achievements;

        private HighlightingOverlay overlay;

        private CommandHighlighter commandHighlighter;

        ViewAdapter(Stage stage) {
            this.stage = stage;
        }

        protected void show() throws RuntimeException {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/stt/gui/jfx/MainWindow.fxml"), localization);
            loader.setController(this);

            BorderPane pane;
            try {
                pane = (BorderPane) loader.load();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            overlay = new HighlightingOverlay(commandText);
            commandHighlighter = new CommandHighlighter(overlay);

            Scene scene = new Scene(pane);

            stage.setScene(scene);
            stage.initStyle(StageStyle.UTILITY);
            stage.setTitle(localization.getString("window.title"));
            Image applicationIcon = new Image("/Logo.png", 32, 32, true, true);
            stage.getIcons().add(applicationIcon);

            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent arg0) {
                    Platform.runLater(new Runnable() {

                        @Override
                        public void run() {
                            shutdown();
                        }
                    });
                }
            });
            scene.setOnKeyPressed(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent event) {
                    if (KeyCode.ESCAPE.equals(event.getCode())) {
                        event.consume();
                        shutdown();
                    }
                }
            });

            stage.show();
            requestFocusOnCommandText();

            commandText.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    overlay.clearHighlights();
                    CharStream input = new ANTLRInputStream(currentCommand.get());
                    EnglishCommandsLexer lexer = new EnglishCommandsLexer(input);
                    TokenStream tokenStream = new CommonTokenStream(lexer);
                    EnglishCommandsParser parser = new EnglishCommandsParser(tokenStream);
                    commandHighlighter.addHighlights(parser.command());
                }
            });

            if (autoCompletionPopup) {
                setupAutoCompletionPopup();
            }
        }

        private void setupAutoCompletionPopup() {
            ObservableList<String> suggestionsForContinuationList = createSuggestionsForContinuationList();
            ListView<String> contentOfAutocompletionPopup = new ListView<>(suggestionsForContinuationList);
            final Popup popup = ContextPopupCreator.createPopupForContextMenu(contentOfAutocompletionPopup, new ContextPopupCreator.ItemSelectionCallback<String>() {
                @Override
                public void selected(String item) {
                    insertAtCaret(item.endsWith(" ") ? item : item + " ");
                }
            });
            suggestionsForContinuationList.addListener(new ListChangeListener<String>() {
                @Override
                public void onChanged(Change<? extends String> c) {
                    if (c.getList().isEmpty()) {
                        popup.hide();
                    } else {
                        popup.show(stage);
                    }
                }
            });
            popup.show(stage);
            new PopupAtCaretPlacer(commandText, popup);
        }

        private ObservableList<String> createSuggestionsForContinuationList() {
            return new ListBinding<String>() {
                @Override
                protected ObservableList<String> computeValue() {
                    List<String> suggestedContinuations = getSuggestedContinuations();
                    Collections.sort(suggestedContinuations);
                    return FXCollections.observableList(suggestedContinuations);
                }

                {
                    bind(commandCaretPosition);
                    bind(currentCommand);
                }


            };
        }

        protected void updateAchievements(final Collection<Achievement> newAchievements) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
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
                }
            });
        }

        protected void shutdown() {
            try {
                stage.close();
            } finally {
                eventBus.post(new ShutdownRequest());
            }
        }

        public void initialize() {
            setupCellFactory();
            final MultipleSelectionModel<TimeTrackingItem> selectionModel = result
                    .getSelectionModel();
            selectionModel.setSelectionMode(SelectionMode.SINGLE);

            bindCaretPosition();
            commandText.textProperty().bindBidirectional(currentCommand);
            result.setItems(filteredList);
            bindItemSelection();
        }

        private void bindItemSelection() {
            result.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    TimeTrackingItem selectedItem = result.getSelectionModel()
                            .getSelectedItem();
                    resultItemSelected(selectedItem);
                }
            });
        }

        private void setupCellFactory() {
            result.setCellFactory(new TimeTrackingItemCellFactory(
                    STTApplication.this, STTApplication.this,
                    STTApplication.this, new TimeTrackingItemFilter() {
                ObservableSet<TimeTrackingItem> firstItemOfDayBinding = new FirstItemOfDaySet(
                        allItems);

                @Override
                public boolean filter(TimeTrackingItem item) {
                    return firstItemOfDayBinding.contains(item);
                }
            }, localization));
        }

        private void bindCaretPosition() {
            commandCaretPosition.addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    commandText.positionCaret(commandCaretPosition.get());
                }
            });
            commandText.caretPositionProperty().addListener(
                    new InvalidationListener() {
                        @Override
                        public void invalidated(Observable observable) {
                            commandCaretPosition.set(commandText
                                    .getCaretPosition());
                        }
                    });
        }

        protected void requestFocusOnCommandText() {
            PlatformImpl.runLater(new Runnable() {
                @Override
                public void run() {
                    commandText.requestFocus();
                }
            });
        }

        protected void updateAllItems(
                final Collection<TimeTrackingItem> updateWith) {
            PlatformImpl.runLater(new Runnable() {
                @Override
                public void run() {
                    allItems.setAll(updateWith);
                    viewAdapter.requestFocusOnCommandText();
                }
            });
        }

        @FXML
        void showReportWindow() {
            try {
                reportWindowBuilder.setupStage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @FXML
        private void done() {
            executeCommand();
            shutdown();
        }

        @FXML
        void insert() {
            boolean executedCommand = executeCommand();
            if (executedCommand) {
                eventBus.post(new ReadItemsRequest());
            }
        }

        @FXML
        private void fin() {
            commandParser.endCurrentItemCommand(new DateTime()).or(NothingCommand.INSTANCE).execute();
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
}
