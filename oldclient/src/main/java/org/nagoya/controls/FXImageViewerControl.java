package org.nagoya.controls;

import io.vavr.collection.Vector;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.system.dialog.FXWindow;

public class FXImageViewerControl extends FXResizeableVBox {
    private final FXImageGridControl control;
    private final FXPagination pagination;

    public FXImageViewerControl(int num) {
        this.control = new FXImageGridControl(num);
        this.control.setOnImageClick((i) -> FXImageFlowWindow.show(this.control.getItem(), i));

        this.pagination = new FXPagination(20);
        this.pagination.setPageFactory(this.control::showPage);

        this.setPadding(FXWindow.getDefaultInset());
        this.getChildren().setAll(this.control.getPane(), this.pagination);

        this.control.getPane().setMinWidth(USE_PREF_SIZE);
        this.control.getPane().prefWidthProperty().bind(this.areaWidthProperty());
        this.control.getPane().setMinHeight(USE_PREF_SIZE);
        this.control.getPane().prefHeightProperty().bind(
                Bindings.createDoubleBinding(() -> this.getAreaHeight() - this.snapSizeY(this.pagination.getHeight()) - 14,
                        this.areaHeightProperty(), this.pagination.heightProperty()));

        this.pagination.prefWidthProperty().bind(this.areaWidthProperty());
    }

    public void setItem(Vector<FxThumb> imageList) {
        this.control.setItem(imageList);
        this.pagination.setPageCount(2147483647);
        this.pagination.setPageCount(this.control.getPageCount());
        this.pagination.setCurrentPageIndex(0);
    }

    @Override
    protected double computeMinWidth(double height) {
        double min = Math.max(400, this.snapSizeX(this.pagination.minWidth(-1)) + this.snappedLeftInset() + this.snappedRightInset());
        this.setStageMinWidth(min);
        return min;
    }

    @Override
    protected double computeMinHeight(double width) {
        double min = this.snapSizeY(this.pagination.minHeight(-1)) + this.snappedTopInset() + this.snappedBottomInset() + 300;
        this.setStageMinHeight(min);
        return min;
    }

    @Override
    protected void layoutChildren() {
        double topInset = this.snappedTopInset();
        double leftInset = this.snappedLeftInset();

        this.layoutInArea(this.pagination, leftInset, topInset, this.getAreaWidth(), this.getAreaHeight(), 0, Insets.EMPTY, true, false, HPos.CENTER, VPos.TOP);
        this.layoutInArea(this.control.getPane(), leftInset, topInset, this.getAreaWidth(), this.getAreaHeight(), 0, Insets.EMPTY, true, false, HPos.CENTER, VPos.BOTTOM);

        //double controlHeight = this.snapSizeY(this.pagination.prefHeight(-1.0D));
        //double pageHeight = this.getAreaHeight() - controlHeight - 14;
        //GUICommon.debugMessage("areaWidth areaHeight " + areaWidth + " " + areaHeight);
        //GUICommon.debugMessage("CONTROL SIZE " + this.getWidth() + " " + this.getHeight());
        //GUICommon.debugMessage("CONTROL PREF SIZE computePrefWidth " + this.computePrefWidth(-1) + " " + this.computePrefHeight(-1));
        //GUICommon.debugMessage("CONTROL PREF SIZE getPrefWidth " + this.getPrefWidth() + " " + this.getPrefHeight());
    }
}
