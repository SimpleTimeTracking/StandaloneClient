package org.stt.gui.jfx;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;


/**
 * Created by dante on 29.03.15.
 */
public class TableBuilder<T> extends Application {
    private TableView<T> table = new TableView<>();

    public static void main(String[] args) {
        Application.launch(TableBuilder.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        VBox root = new VBox();
        Scene scene = new Scene(root);

        TableBuilder<Test> builder = new TableBuilder<Test>();
        builder.withColumn("Test column", "test")
                .withColumn("Text column", "text");

        TableView<Test> tableView = builder.build();
        tableView.setItems(FXCollections.observableList(Arrays.asList(new Test())));

        root.getChildren().addAll(tableView);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public TableView<T> build() {
        try {
            return table;
        } finally {
            table = null;
        }
    }

    public TableBuilder<T> withColumn(String columnName, String property) {
        table.getColumns().add(this.<T>createTableColumn(columnName, property));
        return this;
    }

    private <T> TableColumn<T, Object> createTableColumn(String columnName, String property) {
        TableColumn<T, Object> tableColumn = new TableColumn<>(columnName);
        tableColumn.setCellValueFactory(new PropertyValueFactory<T, Object>(property));
        return tableColumn;
    }

    public static class Test {
        private int test;
        private String text = "Hello world";

        public int getTest() {
            return test;
        }

        public void setTest(int test) {
            this.test = test;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
