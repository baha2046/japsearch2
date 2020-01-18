package org.nagoya.system.dialog;

import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class WindowBuilder {
    @NotNull
    @Contract(" -> new")
    public static WindowBuilder create() {
        return new WindowBuilder();
    }

    private boolean alreadyBuild;

    private boolean releaseOnHide;
    private Runnable runOnClose;
    private String title;
    private boolean showTitle;
    private boolean resizable;
    private boolean sizeToScene;
    private double prefWidth;
    private double prefHeight;
    private StageStyle stageStyle;

    private Region container;

    private WindowBuilder() {
        this.title = "No Name";
        this.stageStyle = StageStyle.TRANSPARENT;
        this.releaseOnHide = true;
        this.resizable = true;
        this.showTitle = true;
        this.sizeToScene = true;
        this.prefWidth = Region.USE_COMPUTED_SIZE;
        this.prefHeight = Region.USE_COMPUTED_SIZE;

        this.runOnClose = () -> {
        };

        this.container = null;
        this.alreadyBuild = false;
    }

    public WindowBuilder runOnClose(Runnable run) {
        this.runOnClose = run;
        return this;
    }

    public WindowBuilder releaseOnHide(boolean b) {
        this.releaseOnHide = b;
        return this;
    }

    public WindowBuilder style(StageStyle style) {
        this.stageStyle = style;
        return this;
    }

    public WindowBuilder body(Region node) {
        this.container = node;
        return this;
    }

    public WindowBuilder title(String title, boolean show) {
        this.title = title;
        this.showTitle = show;
        return this;
    }

    public WindowBuilder resizable(boolean b) {
        this.resizable = b;
        return this;
    }

    public WindowBuilder prefSize(double w, double h) {
        this.sizeToScene = false;
        this.prefWidth = w;
        this.prefHeight = h;
        return this;
    }


    public FXWindow build() {
        if (!Platform.isFxApplicationThread()) {
            throw new RuntimeException("FXWindow - build - not on javafx thread");
        }
        this.alreadyBuild = true;
        return FXWindow.create(this);
    }

    public FXWindow buildSingle() {
        if (!Platform.isFxApplicationThread()) {
            throw new RuntimeException("FXWindow - build - not on javafx thread");
        }
        this.alreadyBuild = true;
        return FXWindow.createSingle(this);
    }

    public String getTitle() {
        return this.title;
    }

    public StageStyle getStageStyle() {
        return this.stageStyle;
    }

    public Runnable getRunOnClose() {
        return this.runOnClose;
    }

    public boolean isReleaseOnHide() {
        return this.releaseOnHide;
    }

    public boolean isShowTitle() {
        return this.showTitle;
    }

    public boolean isResizable() {
        return this.resizable;
    }

    public boolean isSizeToScene() {
        return this.sizeToScene;
    }

    public double getPrefWidth() {
        return this.prefWidth;
    }

    public double getPrefHeight() {
        return this.prefHeight;
    }

    public Region getContainer() {
        return this.container;
    }
}

