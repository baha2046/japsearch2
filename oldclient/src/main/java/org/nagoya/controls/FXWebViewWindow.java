package org.nagoya.controls;

import com.jfoenix.controls.JFXButton;
import io.vavr.control.Option;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.jetbrains.annotations.NotNull;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class FXWebViewWindow {
    private static FXWebViewControl fxWebViewControl = null;
    private static FXWindow window = null;

    private static FXWebViewControl getControl() {
        if (fxWebViewControl == null) {
            fxWebViewControl = new FXWebViewControl();
        }
        return fxWebViewControl;
    }

    public static void show(String strUrl) {
        show(strUrl, Option.none(), Option.none());
    }

    public static void show(String strUrl, Option<Function<WebView, List<JFXButton>>> customButton, Option<Consumer<WebView>> runModify) {
        window = WindowBuilder.create()
                .title("Web Browser", true)
                .body(getControl())
                .prefSize(1100, 550)
                .buildSingle();

        window.show();

        getControl().loadUrl(strUrl, customButton, runModify);
    }

    public static WebEngine newTab(Option<Function<WebView, List<JFXButton>>> customButton, Option<Consumer<WebView>> runModify) {
        return getControl().newTab(customButton, runModify);
    }

    @NotNull
    public static Pane loadWithoutWindow(String strUrl, @NotNull Option<Function<WebView, List<JFXButton>>> customButton, Option<Consumer<WebView>> runModify) {
        if (window != null) {
            window.terminate();
            window = null;
        }
        getControl().loadUrl(strUrl, customButton, runModify);
        return getControl();
    }
}

