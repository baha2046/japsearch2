package org.nagoya.preferences.options;

import com.jfoenix.controls.JFXButton;
import javafx.scene.layout.StackPane;
import org.nagoya.controls.FXEditableButton;
import org.nagoya.preferences.AppSetting;

import java.util.function.Consumer;

public class IntegerOption extends OptionBase<Integer> {
    public IntegerOption(String key, Integer val, Consumer<Integer> func) {
        super(key, val, func);
    }

    @Override
    public Integer getOption() {
        return AppSetting.getInstance().getIntValue(this.keyString, this.defaultValue);
    }

    @Override
    public void updateOption(Integer val) {
        AppSetting.getInstance().setIntValue(this.keyString, val);
    }

    @Override
    public JFXButton toButton(StackPane stackPane) {
        FXEditableButton<Integer> button = new FXEditableButton<>(this.getOptionText(), Integer::parseInt, this.getOption());
        button.setOnEditAction(this::setOption);
        button.setDialogContainer(stackPane);
        button.setMinWidth(this.buttonWidth);
        return button;
    }
}
