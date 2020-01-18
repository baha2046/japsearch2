package org.nagoya.preferences.options;

import com.jfoenix.controls.JFXButton;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

public abstract class OptionBase<T> {
    public final static int DEFAULT_BUTTON_WIDTH = 200;

    protected String keyString = "";
    protected String optionText = "";
    protected final T defaultValue;
    protected final Consumer<T> setFunc;
    protected int buttonWidth = DEFAULT_BUTTON_WIDTH;

    OptionBase(String key, T val, Consumer<T> func) {
        this.defaultValue = val;
        this.keyString = key;
        this.setFunc = func;
    }

    public void setOptionText(String text) {
        this.optionText = text;
    }

    public String getOptionText() {
        return this.optionText;
    }

    public void setButtonWidth(int buttonWidth) {
        this.buttonWidth = buttonWidth;
    }

    public JFXButton toButton() {
        return this.toButton(null);
    }

    public void setOption(T val) {
        this.setFunc.accept(val);
        this.updateOption(val);
    }

    public abstract T getOption();

    public abstract void updateOption(T val);

    public abstract JFXButton toButton(StackPane stackPane);
}
