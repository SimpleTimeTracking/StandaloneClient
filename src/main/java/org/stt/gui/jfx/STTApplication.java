package org.stt.gui.jfx;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.stt.CommandHandler;
import org.stt.analysis.ExpansionProvider;
import org.stt.config.TimeTrackingItemListConfig;
import org.stt.event.ShutdownRequest;
import org.stt.event.messages.ReadItemsEvent;
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
import org.stt.gui.jfx.text.HighlightingOverlay;
import org.stt.model.TimeTrackingItem;
import org.stt.model.TimeTrackingItemFilter;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class STTApplication implements DeleteActionHandler, EditActionHandler,
        ContinueActionHandler {

    private static final Logger LOG = Logger.getLogger(STTApplication.class
            .getName());
    final ObservableList<TimeTrackingItem> allItems = FXCollections
            .observableArrayList();
    final StringProperty currentCommand = new SimpleStringProperty("");
    final IntegerProperty commandCaretPosition = new SimpleIntegerProperty();
    private final CommandHandler commandHandler;
    private final ReportWindowBuilder reportWindowBuilder;
    private final ExpansionProvider expansionProvider;
    private final ResourceBundle localization;
    private final EventBus eventBus;
    ObservableList<TimeTrackingItem> filteredList;
    ViewAdapter viewAdapter;
    private Collection<TimeTrackingItem> tmpItems = new ArrayList<>();
    private Provider<Stage> stageProvider;

    @Inject
    STTApplication(Provider<Stage> stageProvider,
                   EventBus eventBus,
                   CommandHandler commandHandler,
                   ReportWindowBuilder reportWindowBuilder,
                   ExpansionProvider expansionProvider,
                   ResourceBundle resourceBundle,
                   TimeTrackingItemListConfig timeTrackingItemListConfig) {
        this.stageProvider = checkNotNull(stageProvider);
        this.eventBus = checkNotNull(eventBus);
        eventBus.register(this);
        this.expansionProvider = checkNotNull(expansionProvider);
        this.reportWindowBuilder = checkNotNull(reportWindowBuilder);
        this.commandHandler = checkNotNull(commandHandler);
        this.localization = checkNotNull(resourceBundle);
        checkNotNull(timeTrackingItemListConfig);

        filteredList = new TimeTrackingListFilter(allItems, currentCommand,
                timeTrackingItemListConfig.isFilterDuplicatesWhenSearching());

    }

    @Subscribe
    public void receiveItems(ReadItemsEvent event) {
        LOG.info("Received item event " + event);
        if (event.type == ReadItemsEvent.Type.START) {
            tmpItems.clear();
        }
        tmpItems.addAll(event.timeTrackingItems);
        if (event.type == ReadItemsEvent.Type.DONE) {
            viewAdapter.updateAllItems(tmpItems);
            tmpItems = new ArrayList<>();
        }
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

    protected Optional<TimeTrackingItem> executeCommand() {
        final String text = currentCommand.get();
        if (!text.trim().isEmpty()) {
            Optional<TimeTrackingItem> result = commandHandler
                    .executeCommand(text);
            clearCommand();
            return result;
        }
        return Optional.<TimeTrackingItem>absent();
    }

    private void clearCommand() {
        currentCommand.set("");
    }

    public void show() {
        viewAdapter = new ViewAdapter(stageProvider.get());
        viewAdapter.show();
    }

    public void start() {
        PlatformImpl.runAndWait(new Runnable() {
            @Override
            public void run() {
                show();
                // Post initial request to load all items
                eventBus.post(new ReadItemsRequest());
            }
        });
    }

    @Override
    public void continueItem(TimeTrackingItem item) {
        commandHandler.resumeGivenItem(item);
        viewAdapter.shutdown();
    }

    @Override
    public void edit(TimeTrackingItem item) {
        setCommandText(commandHandler.itemToCommand(item), item.getComment().or("").length());
    }

    @Override
    public void delete(TimeTrackingItem item) {
        try {
            commandHandler.delete(checkNotNull(item));
            allItems.remove(item);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public class ViewAdapter {

        private final Stage stage;

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
            stage.close();
            Platform.exit();
            // Required because txExit() above is just swallowed...
            PlatformImpl.tkExit();
            try {
                commandHandler.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
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
            Optional<TimeTrackingItem> item = executeCommand();
            if (item.isPresent()) {
                eventBus.post(new ReadItemsRequest());
            }
        }

        @FXML
        private void fin() {
            commandHandler.endCurrentItem();
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
