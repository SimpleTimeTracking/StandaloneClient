package org.stt.gui.jfx;

import javafx.scene.text.Font;
import net.engio.mbassy.bus.MBassador;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.command.CommandFormatter;
import org.stt.command.CommandHandler;
import org.stt.command.DoNothing;
import org.stt.command.RemoveActivity;
import org.stt.config.CommandTextConfig;
import org.stt.config.TimeTrackingItemListConfig;
import org.stt.fun.AchievementService;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.text.ExpansionProvider;
import org.stt.text.ItemGrouper;
import org.stt.validation.ItemAndDateValidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.stt.LambdaMatcher.mapped;

public class STTApplicationTest {

    private STTApplication sut;
    @Mock
    private CommandFormatter commandFormatter;
    @Mock
    private ExecutorService executorService;
    @Mock
    private ReportWindowBuilder reportWindowBuilder;
    @Mock
    private ItemGrouper grouper;
    @Mock
    private ExpansionProvider expansionProvider;
    @Mock
    private ResourceBundle resourceBundle;
    private boolean shutdownCalled;
    @Mock
    private ItemAndDateValidator itemValidator;
    @Mock
    private TimeTrackingItemQueries timeTrackingItemQueries;
    @Mock
    private AchievementService achievementService;
    @Mock
    private CommandHandler commandHandler;
    private Font fontAwesome = Font.getDefault();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        sut = new STTApplication(new STTOptionDialogs(resourceBundle), new MBassador<>(), commandFormatter, reportWindowBuilder,
                expansionProvider, resourceBundle, new TimeTrackingItemListConfig(), new CommandTextConfig(), itemValidator, timeTrackingItemQueries, achievementService, executorService,
                commandHandler, fontAwesome);

        sut.viewAdapter = sut.new ViewAdapter(null) {

            @Override
            protected void show() throws RuntimeException {
            }

            @Override
            protected void requestFocusOnCommandText() {
            }

            @Override
            protected void updateAllItems(
                    Collection<TimeTrackingItem> updateWith) {
                sut.allItems.setAll(updateWith);
            }

            @Override
            protected void shutdown() {
                shutdownCalled = true;
            }
        };
    }

    @Test
    public void shouldDelegateToExpansionProvider() {
        // GIVEN

        setTextAndPositionCaretAtEnd("test");

        given(expansionProvider.getPossibleExpansions("test")).willReturn(
                Collections.singletonList("blub"));

        // WHEN
        sut.expandCurrentCommand();

        // THEN
        assertThat(sut.currentCommand.get(), is("testblub"));
    }

    @Test
    public void shouldExpandWithinText() {
        // GIVEN

        sut.currentCommand.set("al beta");
        sut.commandCaretPosition.set(2);

        given(expansionProvider.getPossibleExpansions("al")).willReturn(
                Collections.singletonList("pha"));

        // WHEN
        sut.expandCurrentCommand();

        // THEN
        assertThat(sut.currentCommand.get(), is("alpha beta"));
        assertThat(sut.commandCaretPosition.get(), is(5));
    }

    @Test
    public void shouldExpandToCommonPrefix() {
        // GIVEN

        String currentText = "test";
        setTextAndPositionCaretAtEnd(currentText);

        given(expansionProvider.getPossibleExpansions(currentText)).willReturn(
                Arrays.asList("aaa", "aab"));

        // WHEN
        sut.expandCurrentCommand();

        // THEN
        assertThat(sut.currentCommand.get(), is("testaa"));
    }

    private void setTextAndPositionCaretAtEnd(String currentText) {
        sut.currentCommand.set(currentText);
        sut.commandCaretPosition.set(currentText.length());
    }

    @Test
    public void shouldDeleteItemIfRequested() throws IOException {
        // GIVEN
        TimeTrackingItem item = new TimeTrackingItem("", LocalDateTime.now());

        // WHEN
        sut.delete(item);

        // THEN
        verify(commandHandler).removeActivity(argThat(mapped(cmd -> cmd.itemToDelete, is(item))));
    }

    @Test
    public void deletedItemShouldBeRemoved() {
        // GIVEN
        givenExecutorService();
        final TimeTrackingItem item = new TimeTrackingItem("comment",
                LocalDateTime.now());

        sut.allItems.setAll(item);

        // WHEN
        sut.delete(item);

        // THEN
        assertThat(sut.filteredList, not(hasItem(item)));
    }

    @Test
    public void shouldShowReportWindow() throws IOException {
        // GIVEN

        // WHEN
        sut.viewAdapter.showReportWindow();

        // THEN
        verify(reportWindowBuilder).setupStage();
    }

    @Test
    public void shouldClearCommandAreaOnExecuteCommand() throws Exception {
        // GIVEN
        givenCommand("test");
        given(commandFormatter.parse(anyString())).willReturn(new RemoveActivity(new TimeTrackingItem("", LocalDateTime.now())));

        // WHEN
        sut.executeCommand();

        // THEN
        assertThat(sut.currentCommand.get(), equalTo(""));
    }

    @Test
    public void shouldDelegateCommandExecutionToCommandHandler()
            throws Exception {
        // GIVEN
        String testCommand = "test";

        givenCommand(testCommand);
        given(commandFormatter.parse(anyString())).willReturn(new RemoveActivity(new TimeTrackingItem("", LocalDateTime.now())));

        // WHEN
        sut.executeCommand();

        // THEN
        verify(commandFormatter).parse(testCommand);
    }

    @Test
    public void shouldNotCloseWindowOnSimpleCommandExecution() {
        // GIVEN
        givenCommand("Hello World");
        given(commandFormatter.parse(anyString())).willReturn(DoNothing.INSTANCE);

        // WHEN
        sut.executeCommand();

        // THEN
        assertThat(shutdownCalled, is(false));
    }

    private ItemReader givenReaderThatReturns(final TimeTrackingItem item) {
        ItemReader reader = mock(ItemReader.class);
        given(reader.read()).willReturn(Optional.of(item),
                Optional.empty());
        return reader;
    }

    private void givenExecutorService() {
        willAnswer(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).given(executorService).execute(Matchers.any(Runnable.class));
    }

    private void givenCommand(String command) {
        sut.currentCommand.set(command);
    }
}
