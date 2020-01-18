package org.nagoya.fx.scene;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class FXFactory {

    public static MenuItem menuItem(String text, Runnable runnable) {
        var menu = new MenuItem(text);
        menu.setOnAction((e) -> runnable.run());
        return menu;
    }

    public static VBox vbox(double spacing, Node... elements) {
        VBox vBox = new VBox(spacing);
        vBox.getChildren().addAll(elements);
        return vBox;
    }

    public static HBox hbox(double spacing, Node... elements) {
        HBox hBox = new HBox(spacing);
        hBox.getChildren().addAll(elements);
        return hBox;
    }

    public static JFXTextField textField(String text, double width) {
        JFXTextField textField = new JFXTextField(text);
        if (width > 0) {
            textField.setMinWidth(width);
            textField.setMaxWidth(width);
        }
        return textField;
    }

    public static JFXListView<String> textArea(double width, double height, boolean autoScroll) {
        JFXListView<String> textArea = new JFXListView<>();
        textArea.setMinHeight(height);
        textArea.setMaxHeight(height);
        textArea.setMinWidth(width);
        textArea.setMaxWidth(width);
        if (autoScroll) {
            textArea.getItems().addListener((ListChangeListener<String>) c -> textArea.scrollTo(c.getList().size() - 1));
        }
        return textArea;
    }

    public static JFXButton button(String text, EventHandler<ActionEvent> eventHandler) {
        JFXButton jfxButton = new JFXButton(text);
        if (eventHandler != null) {
            jfxButton.setOnAction(eventHandler);
        }
        return jfxButton;
    }

    public static JFXButton buttonWithBorder(String text, EventHandler<ActionEvent> eventHandler) {
        JFXButton jfxButton = button(text, eventHandler);
        jfxButton.setStyle(" -fx-border-color: #AAAAAA; -fx-border-insets: 1; -fx-border-radius: 4;");
        return jfxButton;
    }
}
