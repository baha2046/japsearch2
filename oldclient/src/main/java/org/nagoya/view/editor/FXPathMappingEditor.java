package org.nagoya.view.editor;

import io.vavr.collection.Vector;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.preferences.RenameSettings;
import org.nagoya.system.dialog.DialogBuilder;


public class FXPathMappingEditor {
    public static void show(Runnable runnable) {

        Vector<MappingData> dataVector = Vector.of(RenameSettings.getInstance().getCompany())
                .map(t -> new MappingData(t.substring(0, t.indexOf("|")), t.substring(t.indexOf("|") + 1)));

        //dataVector.forEach(t->GUICommon.debugMessage(t.toString()));

        TableView<MappingData> tableView = new TableView<>();

        TableColumn<MappingData, String> fromColumn = new TableColumn<>("From");
        TableColumn<MappingData, String> toColumn = new TableColumn<>("To");

        fromColumn.setPrefWidth(250);
        toColumn.setPrefWidth(250);
        fromColumn.setCellValueFactory(
                new PropertyValueFactory<>("fromString"));
        toColumn.setCellValueFactory(
                new PropertyValueFactory<>("toString"));
        fromColumn.setCellFactory(arg0 -> new TextFieldTableCell<>(new DefaultStringConverter()));
        fromColumn.setOnEditCommit(t ->
                t.getTableView().getItems().get(t.getTablePosition().getRow()).setFromString(t.getNewValue()));
        toColumn.setCellFactory(arg0 -> new TextFieldTableCell<>(new DefaultStringConverter()));
        toColumn.setOnEditCommit(t ->
                t.getTableView().getItems().get(t.getTablePosition().getRow()).setToString(t.getNewValue()));

        tableView.setMinWidth(520);
        tableView.setMaxHeight(500);
        tableView.setMinHeight(500);
        tableView.setEditable(true);
        tableView.getColumns().addAll(fromColumn, toColumn);

        ObservableList<MappingData> dataObservableList = FXCollections.observableArrayList(dataVector.asJava());
        tableView.setItems(dataObservableList);

        var btnDel = FXFactory.buttonWithBorder("  Delete Selected Mapping  ", (e) -> {
            if (tableView.getSelectionModel().getSelectedIndex() != -1) {
                dataObservableList.remove(tableView.getSelectionModel().getSelectedIndex());
            }
        });

        var vBox = FXFactory.vbox(15, tableView, btnDel);

        Runnable saveAction = () -> {
            RenameSettings.getInstance()
                    .setCompany(Vector.ofAll(dataObservableList)
                            .map(m -> m.fromStringProperty().get() + "|" + m.toStringProperty().get())
                            .toJavaList().toArray(new String[0]));
            RenameSettings.writeSetting();
            runnable.run();
        };

        DialogBuilder.create()
                .heading("[ Mapping editor ]")
                .body(vBox)
                .button("Cancel", "Save Change", saveAction)
                .build()
                .show();
    }

    public static class MappingData {
        private final StringProperty fromString;
        private final StringProperty toString;

        MappingData(String f, String t) {
            this.fromString = new SimpleStringProperty(f);
            this.toString = new SimpleStringProperty(t);
        }

        public StringProperty fromStringProperty() {
            return this.fromString;
        }

        public StringProperty toStringProperty() {
            return this.toString;
        }

        void setToString(String t) {
            this.toString.set(t);
        }

        void setFromString(String f) {
            this.fromString.set(f);
        }
    }

}
