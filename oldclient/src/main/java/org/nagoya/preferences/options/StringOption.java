package org.nagoya.preferences.options;

import com.jfoenix.controls.JFXButton;
import javafx.scene.layout.StackPane;
import org.nagoya.controls.FXEditableButton;
import org.nagoya.preferences.AppSetting;

import java.util.function.Consumer;
import java.util.function.Function;

public class StringOption extends OptionBase<String> {

    public StringOption(String key, String val, Consumer<String> func) {
        super(key, val, func);
    }

    @Override
    public String getOption() {
        return AppSetting.getInstance().getStringValue(this.keyString, this.defaultValue);
    }

    @Override
    public void updateOption(String preferenceValue) {
        AppSetting.getInstance().setStringValue(this.keyString, preferenceValue);
    }

    @Override
    public JFXButton toButton(StackPane stackPane) {
        FXEditableButton<String> button = new FXEditableButton<>(this.getOptionText(), Function.identity(), this.getOption());
        button.setOnEditAction(this::setOption);
        button.setDialogContainer(stackPane);
        button.setMinWidth(this.buttonWidth);
        return button;
    }
}
