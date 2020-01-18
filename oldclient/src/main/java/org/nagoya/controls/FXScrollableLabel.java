package org.nagoya.controls;

import com.jfoenix.controls.JFXTextArea;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;
import org.nagoya.system.dialog.DialogBuilder;

public class FXScrollableLabel {
    private final Label txtDesc;
    private final ScrollPane scrollTxtDesc;
    private Runnable editAction;

    public FXScrollableLabel() {
        this.txtDesc = new Label();
        this.txtDesc.setWrapText(true);
        this.scrollTxtDesc = new FXScrollPane(this.txtDesc);
        this.scrollTxtDesc.setFitToWidth(true);
        this.editAction = this::defaultEditAction;

        this.setTextColor("white");
        this.setBackgroundColor("transparent");
    }

    public void setEditAction(Runnable editAction) {
        this.editAction = editAction;
    }

    public void setPrefSize(double w, double h) {
        this.scrollTxtDesc.setPrefSize(w, h);
    }

    public void setTextColor(String strColor) {
        this.getLabel().setStyle("-fx-text-fill: " + strColor + ";");
    }

    public void setBackgroundColor(String strColor) {
        this.getPane().setStyle("-fx-background-color: " + strColor + ";");
    }

    public void setEditable(boolean editable) {
        if (editable) {
            this.getLabel().setOnMouseClicked((event) -> {
                if (event.getClickCount() == 2) {
                    this.editAction.run();
                }
            });
        } else {
            this.getLabel().setOnMouseClicked(null);
        }
    }

    public void fitToPane(@NotNull Pane pane) {
        this.setPrefSize(pane.getPrefWidth(), pane.getPrefHeight());
        pane.getChildren().add(this.getPane());
    }

    public Label getLabel() {
        return this.txtDesc;
    }

    public ScrollPane getPane() {
        return this.scrollTxtDesc;
    }

    public String getText() {
        return this.getLabel().getText();
    }

    public void setText(String text) {
        this.getLabel().setText(text);
    }

    public StringProperty textProperty() {
        return this.getLabel().textProperty();
    }

    private void defaultEditAction() {
        JFXTextArea textArea = new JFXTextArea(this.getText());
        DialogBuilder.create()
                .body(textArea)
                .buttonYesNo(() -> this.setText(textArea.getText()))
                .build()
                .show();
    }
}
