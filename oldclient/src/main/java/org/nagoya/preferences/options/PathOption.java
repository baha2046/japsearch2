package org.nagoya.preferences.options;

import com.jfoenix.controls.JFXButton;
import io.vavr.control.Option;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.StackPane;
import org.nagoya.controls.FXEditableButton;
import org.nagoya.preferences.GuiSettings;
import org.nagoya.view.dialog.FXSelectPathDialog;

import java.nio.file.Path;

public class PathOption extends OptionBase<Path> {
    private final GuiSettings.Key pathKey;

    public PathOption(GuiSettings.Key key) {
        super("", null, (v) -> {
        });
        this.pathKey = key;
    }

    @Override
    public Path getOption() {
        return GuiSettings.getInstance().getDirectory(this.pathKey);
    }

    @Override
    public void updateOption(Path preferenceValue) {
        GuiSettings.getInstance().setDirectory(this.pathKey, preferenceValue);
    }

    @Override
    public JFXButton toButton(StackPane stackPane) {
        FXEditableButton<Path> button = new FXEditableButton<>("", Path::of, this.getOption());
        button.setOnEditAction((EventHandler<ActionEvent>) (e) -> {
            Option<Path> getPath = FXSelectPathDialog.show(this.getOptionText(), this.getOption());
            getPath.peek(button::setEditableVal);
            getPath.peek(this::setOption);
        });
        button.setMinWidth(this.buttonWidth);
        return button;
    }
}
