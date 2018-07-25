package org.stt.gui.jfx

import com.sun.javafx.geom.BaseBounds
import com.sun.javafx.geom.transform.BaseTransform
import com.sun.javafx.jmx.MXNodeAlgorithm
import com.sun.javafx.jmx.MXNodeAlgorithmContext
import com.sun.javafx.sg.prism.NGNode
import javafx.animation.FadeTransition
import javafx.beans.binding.Bindings
import javafx.beans.property.DoubleProperty
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.concurrent.Callable
import java.util.function.Predicate

internal open class TimeTrackingItemCellWithActions(fontAwesome: Font,
                                                    localization: ResourceBundle,
                                                    private val lastItemOfDay: Predicate<TimeTrackingItem>,
                                                    actionsHandler: ActionsHandler,
                                                    labelToNodeMapper: ActivityTextDisplayProcessor) : ListCell<TimeTrackingItem>() {

    private val cellPane = HBox(2.0)
    val editButton: Button
    val continueButton: Button
    val deleteButton: Button
    val stopButton: Button
    private val lastItemOnDayPane: BorderPane

    private val newDayNode: Node
    private val itemNodes: TimeTrackingItemNodes
    private val actions: HBox

    init {
        itemNodes = TimeTrackingItemNodes(labelToNodeMapper, TIME_FORMATTER, fontAwesome, 450, 180, localization)
        editButton = FramelessButton(glyph(fontAwesome, Glyph.PENCIL, GLYPH_SIZE_MEDIUM))
        continueButton = FramelessButton(glyph(fontAwesome, Glyph.PLAY_CIRCLE, GLYPH_SIZE_MEDIUM, Color.DARKGREEN))
        deleteButton = FramelessButton(glyph(fontAwesome, Glyph.TRASH, GLYPH_SIZE_MEDIUM, Color.web("e26868")))
        stopButton = FramelessButton(glyph(fontAwesome, Glyph.STOP_CIRCLE, GLYPH_SIZE_MEDIUM, Color.GOLDENROD))
        setupTooltips(localization)

        continueButton.setOnAction { actionsHandler.continueItem(item) }
        editButton.setOnAction { actionsHandler.edit(item) }
        deleteButton.setOnAction { actionsHandler.delete(item) }
        stopButton.setOnAction { actionsHandler.stop(item) }

        actions = HBox(continueButton, editButton, deleteButton)
        StackPane.setAlignment(actions, Pos.CENTER)
        val timeOrActions = StackPaneWithoutResize()

        actions.opacityProperty().bind(fadeOnHoverProperty())
        val one = SimpleDoubleProperty(1.0)
        val timePaneOpacity = one.subtract(Bindings.min(one, fadeOnHoverProperty().multiply(2)))
        itemNodes.appendNodesTo(timeOrActions, timePaneOpacity, cellPane.children)
        timeOrActions.children.add(actions)

        cellPane.alignment = Pos.CENTER_LEFT


        lastItemOnDayPane = BorderPane()

        newDayNode = createDateSubheader(fontAwesome)

        contentDisplay = ContentDisplay.GRAPHIC_ONLY
    }

    private fun fadeOnHoverProperty(): DoubleProperty {
        val placeHolder = DummyNode()
        placeHolder.opacity = 0.0

        val fade = FadeTransition()
        fade.fromValue = 0.0
        fade.toValue = 1.0
        fade.node = placeHolder
        cellPane.hoverProperty().addListener { _, _, newValue ->
            fade.rate = (if (newValue) 1 else -1).toDouble()
            fade.play()
        }
        return placeHolder.opacityProperty()
    }

    private fun createDateSubheader(fontAwesome: Font): Node {
        val newDayHbox = HBox(5.0)
        newDayHbox.padding = Insets(2.0)
        val calenderIcon = glyph(fontAwesome, Glyph.CALENDAR)
        calenderIcon.setTextFill(Color.BLACK)
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

    protected open fun setupTooltips(localization: ResourceBundle) {
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
            itemNodes.setItem(item)
            if (lastItemOfDay.test(item)) {
                setupLastItemOfDayPane()
                graphic = lastItemOnDayPane
            } else {
                setupCellPane()
                graphic = cellPane
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

    private class DummyNode : Node() {
        override fun impl_createPeer(): NGNode? {
            return null
        }

        override fun impl_computeGeomBounds(bounds: BaseBounds, tx: BaseTransform): BaseBounds? {
            return null
        }

        override fun impl_computeContains(localX: Double, localY: Double): Boolean {
            return false
        }

        override fun impl_processMXNode(alg: MXNodeAlgorithm, ctx: MXNodeAlgorithmContext): Any? {
            return null
        }
    }

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        private val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    }
}
