package org.stt.gui.jfx;

import javafx.scene.text.Font;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.command.CommandFormatter;
import org.stt.command.CommandHandler;
import org.stt.command.DoNothing;
import org.stt.command.RemoveActivity;
import org.stt.config.ActivitiesConfig;
import org.stt.event.ShuttingDown;
import org.stt.fun.AchievementService;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.query.WorkTimeQueries;
import org.stt.text.ExpansionProvider;
import org.stt.validation.ItemAndDateValidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.stt.LambdaMatcher.mapped;

public class ActivitiesControllerTest {

    private ActivitiesController sut;
    @Mock
    private CommandFormatter commandFormatter;
    @Mock
    private ExecutorService executorService;
    @Mock
    private ExpansionProvider expansionProvider;
    @Mock
    private ItemAndDateValidator itemValidator;
    @Mock
    private TimeTrackingItemQueries timeTrackingItemQueries;
    @Mock
    private AchievementService achievementService;
    @Mock
    private CommandHandler commandHandler;
    private Font fontAwesome = Font.getDefault();
    private boolean shutdownCalled;
    @Mock
    private WorkTimeQueries worktimeQueries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TestFX.installTK();

        ResourceBundle resourceBundle = ResourceBundle.getBundle("org/stt/gui/Application");
        MBassador<Object> eventBus = new MBassador<>(error -> {
        });
        eventBus.subscribe(this);
        WorktimePane worktimePane = new WorktimePane(resourceBundle, eventBus, worktimeQueries);
        sut = new ActivitiesController(new STTOptionDialogs(resourceBundle), eventBus, commandFormatter,
                expansionProvider, resourceBundle, new ActivitiesConfig(), itemValidator,
                timeTrackingItemQueries, achievementService, executorService, commandHandler, fontAwesome,
                worktimePane);
        sut.commandText = new StyleClassedTextArea();
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
        assertThat(sut.commandText.getText(), is("testblub"));
    }

    @Test
    public void shouldExpandWithinText() {
        // GIVEN

        sut.commandText.replaceText("al beta");
        sut.commandText.moveTo(2);

        given(expansionProvider.getPossibleExpansions("al")).willReturn(
                Collections.singletonList("pha"));

        // WHEN
        sut.expandCurrentCommand();

        // THEN
        assertThat(sut.commandText.getText(), is("alpha beta"));
        assertThat(sut.commandText.getCaretPosition(), is(5));
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
        assertThat(sut.commandText.getText(), is("testaa"));
    }

    private void setTextAndPositionCaretAtEnd(String currentText) {
        sut.commandText.replaceText(currentText);
        sut.commandText.moveTo(currentText.length());
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
        verify(commandHandler).removeActivity(argThat(instanceOf(RemoveActivity.class)));
    }

    @Test
    public void shouldClearCommandAreaOnExecuteCommand() throws Exception {
        // GIVEN
        givenCommand("test");
        given(commandFormatter.parse(anyString())).willReturn(new RemoveActivity(new TimeTrackingItem("", LocalDateTime.now())));

        // WHEN
        sut.executeCommand();

        // THEN
        assertThat(sut.commandText.getText(), equalTo(""));
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

    @Handler
    public void shutdownWasCalled(ShuttingDown event) {
        shutdownCalled = true;
    }

    private void givenExecutorService() {
        willAnswer(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).given(executorService).execute(Matchers.any(Runnable.class));
    }

    private void givenCommand(String command) {
        sut.commandText.replaceText(command);
    }
}
