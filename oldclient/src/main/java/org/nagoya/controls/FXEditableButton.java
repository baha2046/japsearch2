package org.nagoya.controls;

import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.nagoya.GUICommon;
import org.nagoya.system.dialog.DialogBuilder;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class FXEditableButton<T> extends JFXButton {
    private final String caption;
    private final Function<T, String> toStringMapper;
    private final Function<String, T> fromStringMapper;
    //private EventHandler<ActionEvent> onEditAction;
    private Consumer<T> consumer;
    private T editVal;
    private StackPane dialogContainer;

    public FXEditableButton(String caption, Function<String, T> fromString, Function<T, String> toString, T val) {
        this.caption = caption;
        this.fromStringMapper = fromString;
        this.toStringMapper = toString;
        this.dialogContainer = null;
        this.setEditableVal(val);

        this.setId("editable-button");
        //this.setStyle(" -fx-border-color: #AAAAAA; -fx-border-insets: 1; -fx-border-radius: 4;");
    }

    public FXEditableButton(String caption, Function<String, T> fromString, T val) {
        this(caption, fromString, Objects::toString, val);
    }

    public void setEditableVal(T val) {
        this.editVal = val;
        this.setText(this.caption + this.toStringMapper.apply(this.editVal));
    }

    public T getEditableVal() {
        return this.editVal;
    }

    public void setDialogContainer(StackPane dialogContainer) {
        this.dialogContainer = dialogContainer;
    }

    public void setOnEditAction(Consumer<T> consumer) {
        this.consumer = consumer;
        this.setOnAction((e) -> this.editAction());
    }

    public void setOnEditAction(EventHandler<ActionEvent> onEditAction) {
        this.setOnAction(onEditAction);
    }

    private void editAction() {
        TextField textField = GUICommon.textFieldWithBorder(this.toStringMapper.apply(this.editVal), 200);
        DialogBuilder.create()
                .body(textField)
                .container(this.dialogContainer)
                .buttonYesNo(() -> {
                    this.setEditableVal(this.fromStringMapper.apply(textField.getText()));
                    this.consumer.accept(this.getEditableVal());
                })
                .build()
                .show();
    }
}
