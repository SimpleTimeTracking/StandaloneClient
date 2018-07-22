package org.stt.gui.jfx

import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window
import org.stt.model.TimeTrackingItem
import java.util.*
import javax.inject.Inject

class DeleteOrKeepDialog @Inject
constructor(val localization: ResourceBundle) {
    enum class Result {
        DELETE, KEEP
    }

    fun show(parent: Window, item: TimeTrackingItem): Result {
        val deleteCancelStage = Stage()
        deleteCancelStage.initModality(Modality.WINDOW_MODAL)
        deleteCancelStage.initOwner(parent)
        deleteCancelStage.title = localization.getString("delete.item.title")
        val vbox = VBox(8.0)
        val buttonBox = HBox(8.0)
        buttonBox.alignment = Pos.CENTER
        val deleteButton = Button(localization.getString("delete"))
        val resultProperty = SimpleObjectProperty<Result>(null)
        deleteButton.setOnAction {
            resultProperty.value = Result.DELETE
            deleteCancelStage.close()
        }
        deleteButton.isMnemonicParsing = true
        val keepButton = with(Button(localization.getString("keep"))) {
            setOnAction {
                resultProperty.value = Result.KEEP
                deleteCancelStage.close()
            }
            isMnemonicParsing = true
            isCancelButton = true
            isDefaultButton = true
            this
        }
        buttonBox.children.addAll(deleteButton, keepButton)
        vbox.children.addAll(Text(String.format(localization.getString("delete.item.text"), item.toString())), buttonBox)
        val deleteCancelScene = Scene(vbox)
        deleteCancelStage.scene = deleteCancelScene
        deleteCancelStage.showAndWait()
        return resultProperty.value
    }
}
