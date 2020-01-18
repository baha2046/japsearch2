package org.nagoya.controls;

import io.vavr.collection.Vector;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.preferences.CustomOptions;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;

public class FXImageViewerWindow {
    public static int IMAGE_PER_PAGE = 35;
    public static DoubleProperty VIEW_WIDTH = new SimpleDoubleProperty(1200);
    public static DoubleProperty VIEW_HEIGHT = new SimpleDoubleProperty(755);

    private static FXImageViewerControl fxImageViewerControl = null;
    private static FXWindow window = null;
    public static final CustomOptions OPTIONS = getOptions();

    @NotNull
    private static CustomOptions getOptions() {
        String cName = FXImageViewerWindow.class.getSimpleName();
        CustomOptions customOptions = new CustomOptions(cName);
        customOptions.addOption(cName + "-imagePerPage", IMAGE_PER_PAGE, (b) -> IMAGE_PER_PAGE = b, "Image Per Page : ");
        customOptions.addRow("");
        customOptions.addOption(cName + "-viewWidth", VIEW_WIDTH, "Width : ");
        customOptions.addOption(cName + "-viewHeight", VIEW_HEIGHT, "Height : ");
        return customOptions;
    }

    public static void show(@NotNull DirectoryEntry parent) {
        Systems.useExecutors(() -> {
            Vector<FxThumb> list = parent.getGalleryImages();
            if (list.length() > 0) {
                GUICommon.runOnFx(() -> show(list));
            }
        });
    }

    public static void show(@NotNull Vector<FxThumb> imageList) {
        if (imageList.length() < 1) {
            return;
        }

        window = WindowBuilder.create()
                .title("Image Viewer", true)
                .body(getControl())
                .runOnClose(() -> {
                    VIEW_WIDTH.set(window.getWidth());
                    VIEW_HEIGHT.set(window.getHeight());
                })
                .prefSize(VIEW_WIDTH.get(), VIEW_HEIGHT.get())
                .resizable(true)
                .buildSingle();

        window.show();

        getControl().setItem(imageList);
    }

    public static void reset() {
        window.terminate();
        fxImageViewerControl = null;
    }

    private static FXImageViewerControl getControl() {
        if (fxImageViewerControl == null) {
            fxImageViewerControl = new FXImageViewerControl(IMAGE_PER_PAGE);
        }
        return fxImageViewerControl;
    }
}

