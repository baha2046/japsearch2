package org.nagoya.controls;

import org.nagoya.system.dialog.WindowBuilder;

public class FXActressDatabaseWindow {
    private static FXActressDatabaseControl fxActressDatabaseControl = null;

    private static FXActressDatabaseControl getControl() {
        if (fxActressDatabaseControl == null) {
            fxActressDatabaseControl = new FXActressDatabaseControl();
        }
        return fxActressDatabaseControl;
    }

    public static void show() {
        WindowBuilder.create()
                .title("Actress Database", true)
                .body(getControl())
                .buildSingle().show();
    }
}

