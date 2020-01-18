package org.nagoya.view;

import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import org.nagoya.GUICommon;

import java.util.function.BiConsumer;

public class FXImageObservableWrapper extends FXImageWrapper<ObjectProperty<Image>> {
    public static final BiConsumer<Image, ObjectProperty<Image>> defaultProcess = (image, imageProperty) -> {
        if (imageProperty != null) {
            GUICommon.runOnFx(() -> imageProperty.setValue(image));
        }
    };

    public FXImageObservableWrapper() {
    }

    public FXImageObservableWrapper(ObjectProperty<Image> imageProperty) {
        this();
        this.setImageTarget(imageProperty);
        this.defaultProcessor();
    }

    public void defaultProcessor() {
        this.customProcessor(defaultProcess);
    }
}
