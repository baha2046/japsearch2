package org.nagoya.view;

import io.vavr.control.Option;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Contract;

import java.util.Objects;
import java.util.function.BiConsumer;

public class FXImageWrapper<T> {
    protected int keyInt;
    protected String keyString;
    protected Option<T> imageTarget = Option.none();
    protected BiConsumer<Image, T> setProcess;

    @Contract(pure = true)
    public FXImageWrapper() {
        this.keyString = "";
    }

    public FXImageWrapper(T imageTarget) {
        this();
        this.setImageTarget(imageTarget);
    }

    public void setImageTarget(T imageTarget) {
        this.imageTarget = Option.of(imageTarget);
    }

    public void customProcessor(BiConsumer<Image, T> setProcess) {
        this.setProcess = setProcess;
    }

    public void setKey(String keyString) {
        this.keyString = keyString;
    }

    public void setKey(int keyInt) {
        this.keyInt = keyInt;
    }

    public boolean setImage(Image image, String keyString, T imageTarget) {
        if (this.keyString.equals(keyString) && Objects.nonNull(this.setProcess)) {
            this.setProcess.accept(image, imageTarget);
            return true;
        }
        return false;
    }

    public boolean setImage(Image image, int keyInt, T imageTarget) {
        if (this.keyInt == keyInt && Objects.nonNull(this.setProcess)) {
            this.setProcess.accept(image, imageTarget);
            return true;
        }
        return false;
    }

    public boolean setImage(Image image, String keyString) {
        return this.setImage(image, keyString, this.imageTarget.getOrNull());
    }

    public boolean setImage(Image image, int keyInt) {
        return this.setImage(image, keyInt, this.imageTarget.getOrNull());
    }
}
