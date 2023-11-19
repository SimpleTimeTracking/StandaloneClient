package org.stt.gui.jfx

import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import org.stt.model.TimeTrackingItem
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class STTOptionDialogs @Inject
constructor(private val localization: ResourceBundle,
            @Named("glyph") private val glyphFont: javafx.scene.text.Font,
            @Named("activityToText") private val labelToNodeMapper: @JvmSuppressWildcards ActivityTextDisplayProcessor) {

    internal fun showDeleteOrKeepDialog(item: TimeTrackingItem): Result {
        val dialog = Dialog<Result>()
        dialog.headerText = localization.getString("deleteItem.title")
        dialog.contentText = String.format(localization.getString("deleteItem.text"), item.activity)
        val apply = ButtonType(localization.getString("delete"), ButtonBar.ButtonData.APPLY)
        dialog.dialogPane.buttonTypes.addAll(apply, ButtonType.CANCEL)
        dialog.setResultConverter { param -> if (param == apply) Result.PERFORM_ACTION else Result.ABORT }
        return dialog.showAndWait()
                .orElse(Result.ABORT)
    }

    internal fun showNoCurrentItemAndItemIsLaterDialog(): Result {
        val dialog = Dialog<Result>()
        dialog.headerText = localization.getString("strangeItem.title")
        dialog.contentText = localization.getString("noCurrentItemWithLateItem.text")
        val apply = ButtonType(localization.getString("add"), ButtonBar.ButtonData.APPLY)
        dialog.dialogPane.buttonTypes.addAll(apply, ButtonType.CANCEL)
        dialog.setResultConverter { param -> if (param == apply) Result.PERFORM_ACTION else Result.ABORT }
        return dialog.showAndWait()
                .orElse(Result.ABORT)
    }

    internal fun showItemCoversOtherItemsDialog(coveredItems: List<TimeTrackingItem>): Result {
        val dialog = Dialog<Result>()
        dialog.headerText = localization.getString("strangeItem.title")
        val dialogPane = dialog.dialogPane
        dialogPane.prefWidth = 800.0
        dialogPane.stylesheets.add("org/stt/gui/jfx/STT.css")

        val coveredItemsList = ListView<TimeTrackingItem>()
        coveredItemsList.setCellFactory {
            val itemNodes = TimeTrackingItemNodes(labelToNodeMapper, DATE_TIME_FORMATTER, glyphFont, 400, 360, localization)
            val cell = object : ListCell<TimeTrackingItem>() {
                override fun updateItem(item: TimeTrackingItem?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (!empty && item != null) {
                        itemNodes.setItem(item)
                    }
                }
            }
            val cellParts = HBox()
            cellParts.alignment = Pos.CENTER_LEFT
            cell.graphic = cellParts
            itemNodes.appendNodesTo(cellParts.children)
            cell
        }
        coveredItemsList.items.setAll(coveredItems)

        val borderPane = BorderPane()
        borderPane.top = Label(localization.getString("itemCoversOtherItems.text"))
        borderPane.center = coveredItemsList

        dialogPane.content = borderPane
        val apply = ButtonType(localization.getString("continue"), ButtonBar.ButtonData.APPLY)
        dialogPane.buttonTypes.addAll(apply, ButtonType.CANCEL)
        dialog.setResultConverter { param -> if (param == apply) Result.PERFORM_ACTION else Result.ABORT }
        return dialog.showAndWait()
                .orElse(Result.ABORT)
    }

    internal fun showRenameDialog(numberOfActivities: Int, before: String, after: String): Result {
        val dialog = Dialog<Result>()
        dialog.headerText = localization.getString("bulkRename.title")
        dialog.dialogPane.content = Label(String.format(localization.getString("bulkRename.text"), numberOfActivities, before, after))
        val apply = ButtonType(localization.getString("rename"), ButtonBar.ButtonData.APPLY)
        dialog.dialogPane.buttonTypes.addAll(apply, ButtonType.NO)
        dialog.setResultConverter { param -> if (param == apply) Result.PERFORM_ACTION else Result.ABORT }
        return dialog.showAndWait()
                .orElse(Result.ABORT)
    }

    enum class Result {
        PERFORM_ACTION, ABORT
    }

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    }
}
