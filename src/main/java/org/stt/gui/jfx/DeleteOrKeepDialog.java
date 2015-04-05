package org.stt.gui.jfx;

import com.google.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.stt.model.TimeTrackingItem;

import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 12.03.15.
 */
public class DeleteOrKeepDialog {
    private ResourceBundle localization;

    public enum Result {
        DELETE, KEEP
    }

    @Inject
    public DeleteOrKeepDialog(ResourceBundle localization) {

        this.localization = checkNotNull(localization);
    }

    public Result show(Window parent, TimeTrackingItem item) {
        final Stage deleteCancelStage = new Stage();
        deleteCancelStage.initModality(Modality.WINDOW_MODAL);
        deleteCancelStage.initOwner(parent);
        deleteCancelStage.setTitle(localization.getString("delete.item.title"));
        VBox vbox = new VBox(8);
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        Button deleteButton = new Button(localization.getString("delete"));
        final SimpleObjectProperty<Result> resultProperty = new SimpleObjectProperty<>(null);
        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                resultProperty.setValue(Result.DELETE);
                deleteCancelStage.close();
            }
        });
        deleteButton.setMnemonicParsing(true);
        Button keepButton = new Button(localization.getString("keep"));
        keepButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                resultProperty.setValue(Result.KEEP);
                deleteCancelStage.close();
            }
        });
        keepButton.setMnemonicParsing(true);
        keepButton.setCancelButton(true);
        keepButton.setDefaultButton(true);
        buttonBox.getChildren().addAll(deleteButton, keepButton);
        vbox.getChildren().addAll(new Text(String.format(localization.getString("delete.item.text"), item.toString())), buttonBox);
        Scene deleteCancelScene = new Scene(vbox);
        deleteCancelStage.setScene(deleteCancelScene);
        deleteCancelStage.showAndWait();
        return resultProperty.getValue();
    }
}
