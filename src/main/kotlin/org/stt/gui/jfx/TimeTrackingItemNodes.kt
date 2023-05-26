package org.stt.gui.jfx

import javafx.beans.binding.DoubleBinding
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import org.controlsfx.validation.decoration.GraphicValidationDecoration
import org.stt.gui.UIMain
import org.stt.gui.jfx.Glyph.Companion.GLYPH_SIZE_MEDIUM
import org.stt.gui.jfx.Glyph.Companion.glyph
import org.stt.model.TimeTrackingItem
import org.stt.time.DateTimes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Created by dante on 01.07.17.
 */
class TimeTrackingItemNodes(private val labelToNodeMapper: @JvmSuppressWildcards ActivityTextDisplayProcessor,
                            private val dateTimeFormatter: DateTimeFormatter,
                            fontAwesome: Font,
                            labelAreaWidth: Int,
                            timePaneWidth: Int,
                            private val localization: ResourceBundle) {

    private val labelForComment = TextFlow()
    private val timePane = HBox()
    private val labelForStart = Label()
    private val labelForEnd = Label()
    private val startToFinishActivityGraphics: Node
    private val ongoingActivityGraphics: Control
    private val space: Pane
    private val labelArea: Pane

    init {
        startToFinishActivityGraphics = glyph(fontAwesome, Glyph.FAST_FORWARD, GLYPH_SIZE_MEDIUM)
        ongoingActivityGraphics = glyph(fontAwesome, Glyph.FORWARD, GLYPH_SIZE_MEDIUM)

        labelArea = StackPaneWithoutResize(labelForComment)
        StackPane.setAlignment(labelForComment, Pos.CENTER_LEFT)

        HBox.setHgrow(labelArea, Priority.ALWAYS)
        labelArea.maxWidth = labelAreaWidth.toDouble()

        space = Pane()
        HBox.setHgrow(space, Priority.SOMETIMES)
        timePane.prefWidth = timePaneWidth.toDouble()
        timePane.spacing = 10.0
        timePane.alignment = Pos.CENTER_LEFT

        if (UIMain.DEBUG_UI) {
            labelArea.border = Border(BorderStroke(Color.RED, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderStroke.MEDIUM))
            labelForComment.border = Border(BorderStroke(Color.GREEN, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderStroke.MEDIUM))
        }
    }

    private fun applyLabelForComment(activity: String) {
        val textNodes = labelToNodeMapper(Stream.of(activity))
                .map<Node> {
                    if (it is String) {
                        return@map Text(it)
                    }
                    if (it is Node) {
                        return@map it
                    }
                    throw IllegalArgumentException(String.format("Unsupported element: %s", it))
                }.toList()

        labelForComment.children.setAll(textNodes)
    }


    fun appendNodesTo(parent: ObservableList<Node>) {
        parent.addAll(labelArea, space, timePane)
    }

    fun appendNodesToAndWrapTimePane(timePaneContainer: Pane, parent: ObservableList<Node>) {
        timePaneContainer.children.add(timePane)
        parent.addAll(labelArea, space, timePaneContainer)
    }

    fun bindTimePaneOpacity(timePaneOpacity: DoubleBinding) {
        timePane.opacityProperty().bind(timePaneOpacity)
    }

    fun setItem(item: TimeTrackingItem) {
        applyLabelForComment(item.activity)
        setupTimePane(item)
    }

    private fun setupTimePane(item: TimeTrackingItem) {
        labelForStart.text = dateTimeFormatter.format(item.start)

        val end = item.end
        if (end != null) {
            labelForEnd.text = dateTimeFormatter.format(end)
            timePane.children.setAll(labelForStart, startToFinishActivityGraphics, labelForEnd)
        } else {
            if (!DateTimes.isOnSameDay(item.start, LocalDateTime.now())) {
                val stackPane = StackPane(ongoingActivityGraphics)
                val ongoingActivityWarning = ImageView(WARNING_IMAGE)
                Tooltips.install(stackPane, localization.getString("ongoingActivityDidntStartToday"))
                stackPane.children.add(ongoingActivityWarning)
                StackPane.setAlignment(ongoingActivityWarning, Pos.TOP_RIGHT)
                timePane.children.setAll(labelForStart, stackPane)
            } else {
                timePane.children.setAll(labelForStart, ongoingActivityGraphics)
            }
        }
    }

    companion object {
        // see also corresponding --add-opens to allow access to this file
        private val WARNING_IMAGE = Image(GraphicValidationDecoration::class.java.getResource("/impl/org/controlsfx/control/validation/decoration-warning.png").toExternalForm()) //$NON-NLS-1$
    }

}
