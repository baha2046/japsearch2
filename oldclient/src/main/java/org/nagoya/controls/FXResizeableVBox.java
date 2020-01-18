package org.nagoya.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FXResizeableVBox extends VBox {

    protected final DoubleProperty areaWidth = new SimpleDoubleProperty(0);
    protected final DoubleProperty areaHeight = new SimpleDoubleProperty(0);

    public FXResizeableVBox() {
        this.areaWidth.bind(Bindings.createDoubleBinding(this::computeAreaWidth, this.widthProperty(), this.insetsProperty()));
        this.areaHeight.bind(Bindings.createDoubleBinding(this::computeAreaHeight, this.heightProperty(), this.insetsProperty()));
    }

    protected double computeAreaWidth() {
        return this.snapSizeX(this.getWidth()) - this.snappedLeftInset() - this.snappedRightInset();
    }

    protected double computeAreaHeight() {
        return this.snapSizeY(this.getHeight()) - this.snappedTopInset() - this.snappedBottomInset();
    }

    @Override
    protected double computePrefWidth(double height) {
        return 0;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 0;
    }

    protected void setStageMinWidth(double width) {
        Scene scene = this.getScene();
        if (scene != null) {
            Stage root = (Stage) scene.getWindow();
            if (root != null && root.getWidth() > 0 && this.getWidth() > 0) {
                double widthOffset = this.snapSizeX(root.getWidth() - this.getWidth());
                root.setMinWidth(width + widthOffset);
            }
        }
    }

    protected void setStageMinHeight(double height) {
        Scene scene = this.getScene();
        if (scene != null) {
            Stage root = (Stage) scene.getWindow();
            if (root != null && root.getHeight() > 0 && this.getHeight() > 0) {
                double heightOffset = this.snapSizeY(root.getHeight() - this.getHeight());
                root.setMinHeight(height + heightOffset);
            }
        }
    }

    public double getAreaWidth() {
        return this.areaWidth.get();
    }

    public DoubleProperty areaWidthProperty() {
        return this.areaWidth;
    }

    public double getAreaHeight() {
        return this.areaHeight.get();
    }

    public DoubleProperty areaHeightProperty() {
        return this.areaHeight;
    }
}
