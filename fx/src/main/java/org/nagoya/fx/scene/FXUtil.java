package org.nagoya.fx.scene;

import javafx.application.Platform;

public class FXUtil {
    public static void runOnFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
