package org.stt.gui.jfx

import javafx.animation.FadeTransition
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import org.stt.gui.jfx.Glyph.Companion.GLYPH_SIZE_MEDIUM
import org.stt.gui.jfx.Glyph.Companion.glyph
import org.stt.model.TimeTrackingItem
import org.stt.submit.SubmitSelectionManager
import org.stt.submit.SubmitStatusTracker
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Predicate

internal open class TimeTrackingItemCellWithActions(fontAwesome: Font,
                                                    localization: ResourceBundle,
                                                    private val lastItemOfDay: Predicate<TimeTrackingItem>,
                                                    actionsHandler: ActionsHandler,
                                                    labelToNodeMapper: ActivityTextDisplayProcessor,
                                                    private val submitStatusTracker: SubmitStatusTracker,
                                                    private val submitSelectionManager: SubmitSelectionManager,
                                                    private val connectorIdProvider: () -> String?) : ListCell<TimeTrackingItem>() {

    private val cellPane = HBox(2.0)
    val editButton: Button
    val continueButton: Button
    val deleteButton: Button
    val stopButton: Button
    private val submitCheckBox: CheckBox
    private val lastItemOnDayPane: BorderPane

    private val newDayNode: Node
    private val itemNodes: TimeTrackingItemNodes
    private val actions: HBox

    init {
        itemNodes = TimeTrackingItemNodes(labelToNodeMapper, TIME_FORMATTER, fontAwesome, 450, 220, localization)
        editButton = FramelessButton(glyph(fontAwesome, Glyph.PENCIL, GLYPH_SIZE_MEDIUM))
        continueButton = FramelessButton(glyph(fontAwesome, Glyph.PLAY_CIRCLE, GLYPH_SIZE_MEDIUM, Color.DARKGREEN))
        deleteButton = FramelessButton(glyph(fontAwesome, Glyph.TRASH, GLYPH_SIZE_MEDIUM, Color.web("e26868")))
        stopButton = FramelessButton(glyph(fontAwesome, Glyph.STOP_CIRCLE, GLYPH_SIZE_MEDIUM, Color.GOLDENROD))
        submitCheckBox = CheckBox()
        setupTooltips(localization)

        continueButton.setOnAction { actionsHandler.continueItem(item) }
        editButton.setOnAction { actionsHandler.edit(item) }
        deleteButton.setOnAction { actionsHandler.delete(item) }
        stopButton.setOnAction { actionsHandler.stop(item) }

        submitCheckBox.setOnAction {
            val connectorId = connectorIdProvider() ?: return@setOnAction
            val currentItem = item ?: return@setOnAction
            submitSelectionManager.setSelected(connectorId, currentItem, submitCheckBox.isSelected)
        }

        actions = HBox(continueButton, editButton, deleteButton)
        StackPane.setAlignment(actions, Pos.CENTER)
        val timeOrActions = StackPaneWithoutResize()

        val fade = addOpacityFadeTransition(actions);
        bindFadeTransitionToHover(fade, cellPane);

        val one = SimpleDoubleProperty(1.0)
        val timePaneOpacity = one.subtract(Bindings.min(one, actions.opacityProperty().multiply(2)))
        itemNodes.appendNodesToAndWrapTimePane(timeOrActions, cellPane.children)
        itemNodes.bindTimePaneOpacity(timePaneOpacity)
        timeOrActions.children.add(actions)

        cellPane.children.add(submitCheckBox)
        cellPane.alignment = Pos.CENTER_LEFT

        lastItemOnDayPane = BorderPane()

        newDayNode = createDateSubheader(fontAwesome)

        contentDisplay = ContentDisplay.GRAPHIC_ONLY
    }

    private fun addOpacityFadeTransition(node: Node) : FadeTransition {
        node.opacity = 0.0

        val fade = FadeTransition()
        fade.fromValue = 0.0
        fade.toValue = 1.0
        fade.node = node
        cellPane.hoverProperty().addListener { _, _, newValue ->
            fade.rate = (if (newValue) 1 else -1).toDouble()
            fade.play()
        }
        return fade;
    }

    private fun bindFadeTransitionToHover(fade : FadeTransition, node: Node) {
        node.hoverProperty().addListener { _, _, newValue ->
            fade.rate = (if (newValue) 1 else -1).toDouble()
            fade.play()
        }
    }

    private fun createDateSubheader(fontAwesome: Font): Node {
        val newDayHbox = HBox(5.0)
        newDayHbox.padding = Insets(2.0)
        val calenderIcon = glyph(fontAwesome, Glyph.CALENDAR)
        calenderIcon.textFill = Color.BLACK
        newDayHbox.children.add(calenderIcon)
        val dayLabel = Label()
        dayLabel.textFill = Color.BLACK
        dayLabel.textProperty().bind(Bindings.createStringBinding(
                Callable { if (itemProperty().get() == null) "" else DATE_FORMATTER.format(itemProperty().get().start) },
                itemProperty()))
        newDayHbox.children.add(dayLabel)
        newDayHbox.background = Background(BackgroundFill(Color.LIGHTSTEELBLUE, null, null))
        return newDayHbox
    }

    fun setupTooltips(localization: ResourceBundle) {
        editButton.tooltip = Tooltip(localization
                .getString("itemList.edit"))
        continueButton.tooltip = Tooltip(localization
                .getString("itemList.continue"))
        deleteButton.tooltip = Tooltip(localization
                .getString("itemList.delete"))
        stopButton.tooltip = Tooltip(localization
                .getString("itemList.stop"))
    }

    public override fun updateItem(item: TimeTrackingItem?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            actions.children[0] = if (item.end != null) continueButton else stopButton

            val isSubmitted = submitStatusTracker.isSubmitted(item)
            editButton.isDisable = isSubmitted
            deleteButton.isDisable = isSubmitted
            continueButton.isDisable = isSubmitted
            stopButton.isDisable = isSubmitted

            val connectorId = connectorIdProvider()
            val isSubmittedToActive = connectorId?.let { submitStatusTracker.isSubmitted(item, it) } ?: false
            submitCheckBox.isSelected = isSubmittedToActive || (connectorId?.let { submitSelectionManager.isSelected(it, item) } ?: false)
            submitCheckBox.isDisable = isSubmittedToActive

            itemNodes.setItem(item)
            graphic = if (lastItemOfDay.test(item)) {
                setupLastItemOfDayPane()
                lastItemOnDayPane
            } else {
                setupCellPane()
                cellPane
            }
        }
    }

    private fun setupCellPane() {
        lastItemOnDayPane.center = null
        lastItemOnDayPane.top = null
    }

    private fun setupLastItemOfDayPane() {
        lastItemOnDayPane.center = cellPane
        lastItemOnDayPane.top = newDayNode
        BorderPane.setMargin(newDayNode, Insets(10.0, 0.0, 10.0, 0.0))
    }

    interface ActionsHandler {
        fun continueItem(item: TimeTrackingItem)

        fun edit(item: TimeTrackingItem)

        fun delete(item: TimeTrackingItem)

        fun stop(item: TimeTrackingItem)
    }

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        private val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    }
}