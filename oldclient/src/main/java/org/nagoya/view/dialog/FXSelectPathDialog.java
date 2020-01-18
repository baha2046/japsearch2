package org.nagoya.view.dialog;

import io.vavr.control.Option;
import javafx.stage.DirectoryChooser;
import org.nagoya.App;

import java.io.File;
import java.nio.file.Path;

public class FXSelectPathDialog {

    public static Option<Path> show(String title, Path initDir) {

        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(initDir.toFile());
        File dir = fileChooser.showDialog(App.getCurrentStage().getOrNull());
        if (dir != null) {
            return Option.of(dir.toPath());
        }

        return Option.none();
    }
}
