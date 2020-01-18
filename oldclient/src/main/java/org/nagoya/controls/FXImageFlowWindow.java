package org.nagoya.controls;

import io.vavr.collection.Vector;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.preferences.CustomOptions;
import org.nagoya.preferences.AppSetting;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;

public class FXImageFlowWindow {
    public static DoubleProperty VIEW_WIDTH = new SimpleDoubleProperty(1200);
    public static DoubleProperty VIEW_HEIGHT = new SimpleDoubleProperty(755);

    public static final CustomOptions OPTIONS = getOptions();

    @NotNull
    private static CustomOptions getOptions() {
        String cName = FXImageFlowWindow.class.getSimpleName();
        CustomOptions customOptions = new CustomOptions(cName);
        customOptions.addOption(cName + "-viewWidth", VIEW_WIDTH, "Width : ");
        customOptions.addOption(cName + "-viewHeight", VIEW_HEIGHT, "Height : ");
        return customOptions;
    }

    private static FXImageFlowControl fxImageFlowControl = null;
    private static FXWindow window = null;

    public static void show(Vector<FxThumb> imageList, int index) {
        if (fxImageFlowControl == null) {
            fxImageFlowControl = new FXImageFlowControl();
        }

        window = WindowBuilder.create()
                .body(fxImageFlowControl)
                .runOnClose(() -> {
                    GUICommon.debugMessage("FXImageFlowControl Close - " + window.getWidth() + " " + window.getHeight());
                    VIEW_WIDTH.set(window.getWidth());
                    VIEW_HEIGHT.set(window.getHeight());
                    AppSetting.getInstance().saveSetting();
                })
                .prefSize(VIEW_WIDTH.get(), VIEW_HEIGHT.get())
                .title("Image Flow View", true)
                .buildSingle()
                .pos(0, 30);

        window.show();

        fxImageFlowControl.setItem(imageList, index);

        GUICommon.debugMessage("FXImageFlowControl Start - " + window.getWidth() + " " + window.getHeight());
    }

    public static void reset() {
        window.terminate();
        fxImageFlowControl = null;
    }
}
