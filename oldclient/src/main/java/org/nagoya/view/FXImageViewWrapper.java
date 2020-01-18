package org.nagoya.view;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.nagoya.GUICommon;

import java.util.function.BiConsumer;

public class FXImageViewWrapper extends FXImageWrapper<ImageView> {
    public static final BiConsumer<Image, ImageView> defaultProcess = (image, imageView) -> {
        if (imageView != null) {
            GUICommon.runOnFx(() -> imageView.setImage(image));
        }
    };

    public FXImageViewWrapper() {
        this.defaultProcessor();
    }

    public FXImageViewWrapper(ImageView imageView) {
        this();
        this.setImageTarget(imageView);
    }

    public void defaultProcessor() {
        this.setProcess = defaultProcess;
    }
}
