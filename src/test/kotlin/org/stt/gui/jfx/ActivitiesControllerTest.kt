package org.stt.gui.jfx

import javafx.scene.text.Font
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.listener.Handler
import org.assertj.core.api.Assertions.assertThat
import org.fxmisc.richtext.StyleClassedTextArea
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willAnswer
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.stt.Matchers.any
import org.stt.Matchers.argThat
import org.stt.command.CommandFormatter
import org.stt.command.CommandHandler
import org.stt.command.DoNothing
import org.stt.command.NewActivity
import org.stt.config.ActivitiesConfig
import org.stt.event.ShuttingDown
import org.stt.gui.jfx.text.CommandHighlighter
import org.stt.model.TimeTrackingItem
import org.stt.query.TimeTrackingItemQueries
import org.stt.query.WorkTimeQueries
import org.stt.text.ExpansionProvider
import org.stt.validation.ItemAndDateValidator
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.stream.Stream

class ActivitiesControllerTest {

    private lateinit var sut: ActivitiesController
    @Mock
    private lateinit var commandFormatter: CommandFormatter
    @Mock
    private lateinit var executorService: ExecutorService
    @Mock
    private lateinit var expansionProvider: ExpansionProvider
    @Mock
    private lateinit var itemValidator: ItemAndDateValidator
    @Mock
    private lateinit var timeTrackingItemQueries: TimeTrackingItemQueries
    @Mock
    private lateinit var commandHandler: CommandHandler
    private lateinit var fontAwesome: Font
    private var shutdownCalled: Boolean = false
    @Mock
    private lateinit var worktimeQueries: WorkTimeQueries

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        TestFX.installTK()
        fontAwesome = Font.getDefault()

        given(itemValidator.validateItemIsFirstItemAndLater(any()))
                .willReturn(true)

        val resourceBundle = ResourceBundle.getBundle("org/stt/gui/Application")
        val eventBus = MBassador<Any> { }
        eventBus.subscribe(this)
        val worktimePane = WorktimePane(resourceBundle, eventBus, worktimeQueries)
        val activitiesConfig = ActivitiesConfig()
        activitiesConfig.isAskBeforeDeleting = false
        activitiesConfig.isDeleteClosesGaps = false
        val labelToNodeMapper = { it: Any -> Stream.of(it) }
        sut = ActivitiesController(STTOptionDialogs(resourceBundle, fontAwesome, labelToNodeMapper), eventBus, commandFormatter,
                setOf(expansionProvider), resourceBundle, activitiesConfig, itemValidator,
                timeTrackingItemQueries, executorService, commandHandler, fontAwesome,
                worktimePane, labelToNodeMapper, CommandHighlighter.Factory({ emptyList() }))
        sut.commandText = StyleClassedTextArea()
    }

    @Test
    fun shouldDelegateToExpansionProvider() {
        // GIVEN

        setTextAndPositionCaretAtEnd("test")

        given(expansionProvider.getPossibleExpansions("test")).willReturn(
                listOf("blub"))

        // WHEN
        sut.expandCurrentCommand()

        // THEN
        assertThat(sut.commandText.text).isEqualTo("testblub")
    }

    @Test
    fun shouldExpandWithinText() {
        // GIVEN

        sut.commandText.replaceText("al beta")
        sut.commandText.moveTo(2)

        given(expansionProvider.getPossibleExpansions("al")).willReturn(
                listOf("pha"))

        // WHEN
        sut.expandCurrentCommand()

        // THEN
        assertThat(sut.commandText.text).isEqualTo("alpha beta")
        assertThat(sut.commandText.caretPosition).isEqualTo(5)
    }

    @Test
    fun shouldExpandToCommonPrefix() {
        // GIVEN

        val currentText = "test"
        setTextAndPositionCaretAtEnd(currentText)

        given(expansionProvider.getPossibleExpansions(currentText)).willReturn(
                Arrays.asList("aaa", "aab"))

        // WHEN
        sut.expandCurrentCommand()

        // THEN
        assertThat(sut.commandText.text).isEqualTo("testaa")
    }

    private fun setTextAndPositionCaretAtEnd(currentText: String) {
        sut.commandText.replaceText(currentText)
        sut.commandText.moveTo(currentText.length)
    }

    @Test
    fun shouldDeleteItemIfRequested() {
        // GIVEN
        val item = TimeTrackingItem("", LocalDateTime.now())

        // WHEN
        sut.delete(item)

        // THEN
        verify(commandHandler).removeActivity(argThat { it.itemToDelete == item })
    }

    @Test
    fun deletedItemShouldBeRemoved() {
        // GIVEN
        givenExecutorService()
        val item = TimeTrackingItem("comment",
                LocalDateTime.now())

        sut.allItems.setAll(item)

        // WHEN
        sut.delete(item)

        // THEN
        verify<CommandHandler>(commandHandler).removeActivity(argThat { true })
    }

    @Test
    fun shouldClearCommandAreaOnExecuteCommand() {
        // GIVEN
        given(timeTrackingItemQueries.queryItems(any())).willReturn(Stream.of())
        givenCommand("test")
        given(commandFormatter.parse(anyString())).willReturn(NewActivity(TimeTrackingItem("", LocalDateTime.now())))

        // WHEN
        sut.executeCommand()

        // THEN
        assertThat(sut.commandText.text).isEmpty()
    }

    @Test
    fun shouldNotCloseWindowOnSimpleCommandExecution() {
        // GIVEN
        givenCommand("Hello World")
        given(commandFormatter.parse(anyString())).willReturn(DoNothing)

        // WHEN
        sut.executeCommand()

        // THEN
        assertThat(shutdownCalled).isFalse()
    }

    @Handler
    fun shutdownWasCalled(event: ShuttingDown) {
        shutdownCalled = true
    }

    private fun givenExecutorService() {
        willAnswer { invocation ->
            (invocation.arguments[0] as Runnable).run()
            null
        }.given<ExecutorService>(executorService).execute(ArgumentMatchers.any(Runnable::class.java))
    }

    private fun givenCommand(command: String) {
        sut.commandText.replaceText(command)
    }
}
