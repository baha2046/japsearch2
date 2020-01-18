package org.nagoya.preferences.options;

import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.StackPane;
import org.nagoya.controls.FXEditableButton;
import org.nagoya.preferences.AppSetting;

import java.util.function.Consumer;

public class BooleanOption extends OptionBase<Boolean> {
    public BooleanOption(String key, Boolean val, Consumer<Boolean> func) {
        super(key, val, func);
    }

    @Override
    public Boolean getOption() {
        return AppSetting.getInstance().getBooleanValue(this.keyString, this.defaultValue);
    }

    @Override
    public void updateOption(Boolean val) {
        AppSetting.getInstance().setBooleanValue(this.keyString, val);
    }

    @Override
    public JFXButton toButton(StackPane stackPane) {
        FXEditableButton<Boolean> button = new FXEditableButton<>(this.getOptionText(), Boolean::parseBoolean, this.getOption());
        button.setOnEditAction((EventHandler<ActionEvent>) (e) -> {
            button.setEditableVal(!button.getEditableVal());
            this.setOption(button.getEditableVal());
        });
        button.setMinWidth(this.buttonWidth);
        return button;
    }
}
