package org.nagoya.view.editor;

import com.jfoenix.controls.JFXListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.nagoya.model.MovieV2;
import org.nagoya.preferences.RenameSettings;
import org.nagoya.system.dialog.DialogBuilder;

public class FXRenameFormatEditor {

    public static void show(Type type) {
        JFXListView<String> element = new JFXListView<>();
        JFXListView<String> elementSelected = new JFXListView<>();

        ObservableList<String> elementList = FXCollections.observableArrayList();
        ObservableList<String> elementSelectedList = FXCollections.observableArrayList();

        elementList.addAll(MovieV2.getSupportRenameElement());
        if (type == Type.DIRECTORY_NAME) {
            elementSelectedList.addAll(RenameSettings.getInstance().getRenameDirectoryFormat());
        } else {
            elementSelectedList.addAll(RenameSettings.getInstance().getRenameFileFormat());
        }

        element.setOrientation(Orientation.HORIZONTAL);
        element.setMinWidth(800);
        element.setMinHeight(60);
        element.setMaxHeight(60);
        elementSelected.setOrientation(Orientation.HORIZONTAL);
        elementSelected.setMinWidth(800);
        elementSelected.setMinHeight(60);
        elementSelected.setMaxHeight(60);
        element.setItems(elementList);
        elementSelected.setItems(elementSelectedList);

        element.setOnMouseClicked((event) -> {
            if (event.getClickCount() == 2) {
                elementSelectedList.add(elementList.get(element.getSelectionModel().getSelectedIndex()));
            }
        });

        elementSelected.setOnMouseClicked((event) -> {
            if (event.getClickCount() == 2) {
                if (elementSelectedList.size() > 1) {
                    elementSelectedList.remove(elementSelected.getSelectionModel().getSelectedIndex());
                }
            }
        });

        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.setPrefWidth(850);

        vBox.getChildren().addAll(new Text("Element Support"), element, new Text("Current Format"), elementSelected);

        Runnable applyAction = () -> {
            if (type == Type.DIRECTORY_NAME) {
                RenameSettings.getInstance().setRenameDirectoryFormat(elementSelectedList.toArray(new String[0]));
            } else {
                RenameSettings.getInstance().setRenameFileFormat(elementSelectedList.toArray(new String[0]));
            }
            RenameSettings.writeSetting();
        };

        DialogBuilder.create()
                .heading("[ Formatter ]")
                .body(vBox)
                .button("Cancel", "Apply", applyAction)
                .build()
                .show();
    }

    public enum Type {
        DIRECTORY_NAME, FILE_NAME
    }
}
