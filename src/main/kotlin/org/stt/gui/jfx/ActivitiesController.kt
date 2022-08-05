package org.stt.gui.jfx

import javafx.scene.control.skin.VirtualFlow
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyCombination.CONTROL_DOWN
import javafx.scene.layout.*
import javafx.scene.text.Font
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.listener.Handler
import net.engio.mbassy.listener.Listener
import net.engio.mbassy.listener.References
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.StyleClassedTextArea
import org.fxmisc.wellbehaved.event.EventPattern.keyPressed
import org.fxmisc.wellbehaved.event.InputMap.consume
import org.fxmisc.wellbehaved.event.InputMap.sequence
import org.fxmisc.wellbehaved.event.Nodes
import org.stt.States
import org.stt.Streams
import org.stt.Strings.commonPrefix
import org.stt.command.*
import org.stt.config.ActivitiesConfig
import org.stt.event.ShuttingDown
import org.stt.gui.jfx.STTOptionDialogs.Result
import org.stt.gui.jfx.TimeTrackingItemCellWithActions.ActionsHandler
import org.stt.gui.jfx.binding.MappedSetBinding
import org.stt.gui.jfx.binding.TimeTrackingListFilter
import org.stt.gui.jfx.text.CommandHighlighter
import org.stt.model.ItemModified
import org.stt.model.ItemReplaced
import org.stt.model.TimeTrackingItem
import org.stt.query.Criteria
import org.stt.query.TimeTrackingItemQueries
import org.stt.text.ExpansionProvider
import org.stt.validation.ItemAndDateValidator
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.inject.Inject
import javax.inject.Named
import javax.security.auth.callback.Callback
import kotlin.streams.toList

class ActivitiesController @Inject
internal constructor(private val sttOptionDialogs: STTOptionDialogs, // NOSONAR
                     private val eventBus: MBassador<Any>,
                     private val commandFormatter: CommandFormatter,
                     private val expansionProviders: Collection<@JvmSuppressWildcards ExpansionProvider>,
                     private val localization: ResourceBundle,
                     private val activitiesConfig: ActivitiesConfig,
                     private val validator: ItemAndDateValidator,
                     private val queries: TimeTrackingItemQueries,
                     private val executorService: ExecutorService,
                     private val activities: CommandHandler,
                     @param:Named("glyph") private val fontAwesome: Font,
                     private val worktimePane: WorktimePane,
                     @param:Named("activityToText") private val labelToNodeMapper: @JvmSuppressWildcards ActivityTextDisplayProcessor,
                     private val commandHighlighterFactory: CommandHighlighter.Factory) : ActionsHandler {
    internal val allItems = FXCollections
            .observableArrayList<TimeTrackingItem>()
    private val filterDuplicatesWhenSearching = activitiesConfig.isFilterDuplicatesWhenSearching
    internal lateinit var commandText: StyleClassedTextArea

    @FXML
    private lateinit var activityList: ListView<TimeTrackingItem>
    @FXML
    private lateinit var additionals: VBox
    @FXML
    private lateinit var commandPane: BorderPane
    @FXML
    private lateinit var activityListToolbar: ToolBar

    private val suggestedContinuations: List<String>
        get() {
            val textToExpand = textFromStartToCaret
            return expansionProviders
                    .flatMap { expansionProvider -> expansionProvider.getPossibleExpansions(textToExpand) }
        }

    private val textFromStartToCaret: String
        get() = commandText.getText(0, commandText.caretPosition)

    private val panel: BorderPane by lazy {
        loadAndInjectFXML()
    }

    val node: Node get() = panel

    @Handler
    fun onItemChange(event: ItemModified) {
        updateItems()
    }

    private fun setCommandText(textToSet: String, selectionStart: Int = textToSet.length, selectionEnd: Int = textToSet.length) {
        with(commandText) {
            replaceText(textToSet)
            selectRange(selectionStart, selectionEnd)
            requestFocus()
        }
    }

    private fun insertAtCaret(text: String) {
        val caretPosition = commandText.caretPosition
        commandText.insertText(caretPosition, text)
        commandText.moveTo(caretPosition + text.length)
    }

    internal fun expandCurrentCommand() {
        val expansions = suggestedContinuations
        if (!expansions.isEmpty()) {
            var maxExpansion = expansions[0]
            for (exp in expansions) {
                maxExpansion = commonPrefix(maxExpansion, exp)
            }
            insertAtCaret(maxExpansion)
        }
    }

    internal fun executeCommand() {
        val text = commandText.text
        if (text.trim { it <= ' ' }.isEmpty()) {
            return
        }
        commandFormatter
                .parse(text)
                .accept(ValidatingCommandHandler())
        Platform.runLater { commandText.requestFocus() }
    }

    private fun updateItems() {
        CompletableFuture
                .supplyAsync { queries.queryAllItems().toList() }
                .thenAcceptAsync({ allItems.setAll(it) }, { Platform.runLater(it) })
    }

    override fun continueItem(item: TimeTrackingItem) {
        LOG.fine { "Continuing item: $item" }
        activities.resumeActivity(ResumeActivity(item, LocalDateTime.now()))
        clearCommand()

        if (activitiesConfig.isCloseOnContinue) {
            shutdown()
        }
    }

    private fun shutdown() {
        eventBus.publish(ShuttingDown())
    }

    override fun edit(item: TimeTrackingItem) {
        LOG.fine { "Editing item: $item" }
        setCommandText(commandFormatter.asNewItemCommandText(item), 0, item.activity.length)
    }

    override fun delete(item: TimeTrackingItem) {
        LOG.fine { "Deleting item: $item" }
        if (!activitiesConfig.isAskBeforeDeleting || sttOptionDialogs.showDeleteOrKeepDialog(item) == Result.PERFORM_ACTION) {
            val command = RemoveActivity(item)
            if (activitiesConfig.isDeleteClosesGaps) {
                activities.removeActivityAndCloseGap(command)
            } else {
                activities.removeActivity(command)
            }
        }
    }

    override fun stop(item: TimeTrackingItem) {
        LOG.fine { "Stopping item: $item" }
        States.requireThat(item.end == null, "Item to finish is already finished")
        activities.endCurrentActivity(EndCurrentItem(LocalDateTime.now()))

        if (activitiesConfig.isCloseOnStop) {
            shutdown()
        }
    }

    @FXML
    fun initialize() {

        addWorktimePanel()
        addCommandText()
        addInsertButton()
        addNavigationButtonsForActivitiesList()

        val filteredList = TimeTrackingListFilter(allItems, commandText.textProperty(),
                filterDuplicatesWhenSearching)


        val lastItemOfDay = MappedSetBinding(
                Supplier { lastItemOf(filteredList.stream()) }, filteredList)

        setupCellFactory(Predicate { lastItemOfDay.contains(it) })
        with(activityList) {
            selectionModel.selectionMode = SelectionMode.SINGLE
            items = filteredList
        }
        bindItemSelection()

        executorService.execute {
            // Post initial request to load all items
            updateItems()
        }
    }

    private fun addNavigationButtonsForActivitiesList() {
        val space = Region()
        HBox.setHgrow(space, Priority.ALWAYS)

        val oneWeekDownBtn = FramelessButton(Glyph.glyph(fontAwesome, Glyph.ANGLE_DOUBLE_DOWN, 20.0))
        oneWeekDownBtn.setOnAction {
            val virtualFlow = activityList.childrenUnmodifiable[0] as VirtualFlow<*>
            val lastVisibleCell = virtualFlow.lastVisibleCell
            var index = lastVisibleCell.index
            val item = lastVisibleCell.item as TimeTrackingItem
            val dateOfLastVisibleItem = item.start.toLocalDate()
            while (index < activityList.items.size) {
                val currentItem = activityList.items[index]
                if (ChronoUnit.DAYS.between(currentItem.start.toLocalDate(), dateOfLastVisibleItem) >= 7) {
                    break
                }
                index++
            }
            activityList.scrollTo(index)
        }
        Tooltip.install(oneWeekDownBtn, Tooltip(localization.getString("activities.list.weekDown")))
        val oneWeekUpBtn = FramelessButton(Glyph.glyph(fontAwesome, Glyph.ANGLE_DOUBLE_UP, 20.0))
        oneWeekUpBtn.setOnAction {
            val virtualFlow = activityList.childrenUnmodifiable[0] as VirtualFlow<*>
            val lastVisibleCell = virtualFlow.firstVisibleCell
            var index = lastVisibleCell.index
            val item = lastVisibleCell.item as TimeTrackingItem
            val dateOfLastVisibleItem = item.start.toLocalDate()
            while (index >= 0) {
                val currentItem = activityList.items[index]
                if (ChronoUnit.DAYS.between(dateOfLastVisibleItem, currentItem.start.toLocalDate()) >= 7) {
                    break
                }
                index--
            }
            activityList.scrollTo(index)
        }
        Tooltip.install(oneWeekUpBtn, Tooltip(localization.getString("activities.list.weekUp")))
        activityListToolbar.items
                .addAll(space,
                        oneWeekDownBtn,
                        oneWeekUpBtn)
    }

    private fun addWorktimePanel() {
        additionals.children.add(worktimePane)
    }

    private fun addCommandText() {
        val textArea = StyleClassedTextArea()
        textArea.requestFocus()
        textArea.id = "commandText"

        val commandHighlighter = commandHighlighterFactory.create(textArea)
        textArea.textProperty().addListener { _, _, _ -> commandHighlighter.update() }

        commandPane.center = VirtualizedScrollPane(textArea)
        Tooltip.install(textArea, Tooltip(localization.getString("activities.command.tooltip")))
        Nodes.addInputMap(textArea, sequence(
                consume(keyPressed(ENTER, CONTROL_DOWN)) { executeCommand() },
                consume(keyPressed(SPACE, CONTROL_DOWN)) { expandCurrentCommand() },
                consume(keyPressed(F1)) { help() }))
        commandText = textArea
    }

    private fun help() {
        executorService.execute {
            try {
                Desktop.getDesktop().browse(URI(WIKI_URL))
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, "Couldn't open help page", ex)
            } catch (ex: URISyntaxException) {
                LOG.log(Level.SEVERE, "Couldn't open help page", ex)
            }
        }
    }

    private fun addInsertButton() {
        val glyph = Glyph.glyph(fontAwesome, Glyph.ARROW_CIRCLE_RIGHT, 60.0)
        glyph.id = "insert"
        val insertButton = FramelessButton(glyph)
        insertButton.background = commandText.background
        insertButton.tooltip = Tooltip(localization.getString("activities.command.insert"))
        insertButton.setOnAction { executeCommand() }
        BorderPane.setAlignment(insertButton, Pos.CENTER)
        with(commandPane) {
            right = insertButton
            background = commandText.background
        }
    }

    private fun bindItemSelection() {
        activityList.setOnMouseClicked {
            val selectedItem = activityList.selectionModel
                    .selectedItem
            resultItemSelected(selectedItem)
        }
    }

    private fun resultItemSelected(item: TimeTrackingItem?) {
        if (item != null) {
            val textToSet = item.activity
            textOfSelectedItem(textToSet)
        }
    }

    private fun textOfSelectedItem(textToSet: String) {
        setCommandText(textToSet)
        commandText.requestFocus()
    }

    private fun lastItemOf(itemsToProcess: Stream<TimeTrackingItem>): Set<TimeTrackingItem> {
        return itemsToProcess.filter(Streams.distinctByKey { item ->
            item.start.toLocalDate()
        }).collect(Collectors.toSet())
    }

    private fun setupCellFactory(lastItemOfDay: Predicate<TimeTrackingItem>) {
        activityList.cellFactory = object : Callback, javafx.util.Callback<ListView<TimeTrackingItem>, ListCell<TimeTrackingItem>> {
            override fun call(p0: ListView<TimeTrackingItem>?): ListCell<TimeTrackingItem> {
                return TimeTrackingItemCellWithActions(fontAwesome, localization, lastItemOfDay, this@ActivitiesController, labelToNodeMapper)
            }
        }
    }

    private fun loadAndInjectFXML(): BorderPane {
        eventBus.subscribe(this)
        eventBus.subscribe(BulkRenameHelper())

        val loader = FXMLLoader(javaClass.getResource(
                "/org/stt/gui/jfx/ActivitiesPanel.fxml"), localization)
        loader.setController(this)

        return loader.load()
    }

    private fun clearCommand() {
        commandText.clear()
    }

    private inner class ValidatingCommandHandler : CommandHandler {
        override fun addNewActivity(command: NewActivity) {
            val newItem = command.newItem
            val start = newItem.start
            if (!validateItemIsFirstItemAndLater(start) || !validateItemWouldCoverOtherItems(newItem)) {
                return
            }
            activities.addNewActivity(command)
            clearCommand()
        }

        override fun endCurrentActivity(command: EndCurrentItem) {
            activities.endCurrentActivity(command)
            clearCommand()
        }

        override fun removeActivity(command: RemoveActivity) {
            throw IllegalStateException()
        }

        override fun removeActivityAndCloseGap(command: RemoveActivity) {
            throw IllegalStateException()
        }

        override fun resumeActivity(command: ResumeActivity) {
            throw IllegalStateException()
        }

        override fun resumeLastActivity(command: ResumeLastActivity) {
            throw IllegalStateException()
        }

        override fun bulkChangeActivity(itemsToChange: Collection<TimeTrackingItem>, activity: String) {
            throw IllegalStateException()
        }

        private fun validateItemIsFirstItemAndLater(start: LocalDateTime): Boolean {
            return validator.validateItemIsFirstItemAndLater(start) || sttOptionDialogs.showNoCurrentItemAndItemIsLaterDialog() == Result.PERFORM_ACTION
        }

        private fun validateItemWouldCoverOtherItems(newItem: TimeTrackingItem): Boolean {
            val criteria = Criteria()
            criteria.withStartNotBefore(newItem.start)
            criteria.withActivityIsNot(newItem.activity)
            newItem.end?.let { criteria.withEndNotAfter(it) }
            var notSameInterval = Predicate<TimeTrackingItem> { newItem.sameStartAs(it) }
            notSameInterval = notSameInterval.and { newItem.sameEndAs(it) }
            notSameInterval = notSameInterval.negate()
            val coveredItems = queries.queryItems(criteria).filter(notSameInterval).toList()

            return coveredItems.isEmpty() || sttOptionDialogs.showItemCoversOtherItemsDialog(coveredItems) == Result.PERFORM_ACTION
        }
    }

    @Listener(references = References.Strong)
    private inner class BulkRenameHelper {
        private var updating: Boolean = false

        @Handler
        fun onItemReplaced(event: ItemReplaced) {
            if (updating) {
                return
            }
            updating = true
            try {
                val beforeUpdate = event.beforeUpdate
                val afterUpdate = event.afterUpdate
                if (!beforeUpdate.sameStartAs(afterUpdate)
                        || beforeUpdate.sameActivityAs(afterUpdate)
                        || !sameEndAndWasNotOngoing(beforeUpdate, afterUpdate)) {
                    return
                }
                val criteria = Criteria().withActivityIs(beforeUpdate.activity)
                val activityItems = queries.queryItems(criteria).toList()
                if (activityItems.isEmpty()) {
                    return
                }
                val renameResult = sttOptionDialogs.showRenameDialog(activityItems.size, beforeUpdate.activity, afterUpdate.activity)
                if (Result.PERFORM_ACTION == renameResult) {
                    activities.bulkChangeActivity(activityItems, event.afterUpdate.activity)
                }
            } finally {
                updating = false
            }
        }

        private fun sameEndAndWasNotOngoing(beforeUpdate: TimeTrackingItem, afterUpdate: TimeTrackingItem): Boolean {
            return beforeUpdate.sameEndAs(afterUpdate) && beforeUpdate.end != null
        }
    }

    companion object {

        private val LOG = Logger.getLogger(ActivitiesController::class.java
                .name)
        private const val WIKI_URL = "https://github.com/SimpleTimeTracking/StandaloneClient/wiki/CLI"
    }
}
