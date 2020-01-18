package org.nagoya.controls;

import io.vavr.Tuple2;
import io.vavr.collection.Vector;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.model.dataitem.FxThumb;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.nio.file.Path;

class FXImageGridView extends VBox {
    private Vector<ImageView> imageViews = Vector.empty();
    private Vector<HBox> hBoxes = Vector.empty();

    private final DoubleProperty imgWidth = new SimpleDoubleProperty();
    private final DoubleProperty imgHeight = new SimpleDoubleProperty();

    int col;
    int row;
    int maxCol;
    int maxRow;

    FXImageGridControl fxImageGridControl;

    @NotNull
    public static FXImageGridView create(FXImageGridControl control, int col, int row) {
        FXImageGridView fxImageGridView = new FXImageGridView(control);
        fxImageGridView.init(col, row);
        return fxImageGridView;
    }

    FXImageGridView(@NotNull FXImageGridControl control) {
        this.fxImageGridControl = control;
        //this.maxWidthProperty().bind(control.viewWidthProperty());
        //this.maxHeightProperty().bind(control.viewHeightProperty());
    }

    private void init(int col, int row) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(1);
        this.col = 0;
        this.row = 0;
        this.maxCol = col;
        this.maxRow = row;

        this.hBoxes = Vector.range(0, this.maxRow).map(i -> {
            HBox hbox = GUICommon.hbox(1);
            hbox.setAlignment(Pos.CENTER);
            this.getChildren().add(hbox);
            return hbox;
        });

        this.imageViews = Vector.range(0, this.maxCol * this.maxRow).map(i -> {
            ImageView view = new ImageView();
            view.setPreserveRatio(true);
            return view;
        });
    }

    public int getMaxCol() {
        return this.maxCol;
    }

    public int getMaxRow() {
        return this.maxRow;
    }

    public int getCol() {
        return this.col;
    }

    public int getRow() {
        return this.row;
    }

    private void clearViews() {
        //GUICommon.debugMessage("Clear View " + this.imageViews.size());
        this.hBoxes.forEach(hBox -> hBox.getChildren().clear());

        this.imageViews.forEach((view) -> {
            view.setImage(FxThumb.EMPTY_IMAGE);
            view.setVisible(false);
        });
    }

    public void set(@NotNull Vector<Tuple2<FxThumb, Integer>> imageList, int vCol, int vRow) {
        Vector<FxThumb> items = imageList.map(Tuple2::_1);
        this.col = vCol;
        this.row = vRow;
        this.clearViews();

        this.imgWidth.set(this.getMaxWidth() / this.col);
        this.imgHeight.set(this.getMaxHeight() / this.row);

        GUICommon.debugMessage("getMinHeight" + this.getMinHeight());
        GUICommon.debugMessage("getPrefHeight" + this.getPrefHeight());
        GUICommon.debugMessage("getMaxHeight" + this.getMaxHeight());
        GUICommon.debugMessage("getHeight" + this.getHeight());

        Vector<Mono<Image>> task = items.zipWithIndex((t, i) -> {
            this.imageViews.get(i).setOnMouseClicked((e) -> this.fxImageGridControl.getOnImageClick().accept(imageList.get(i)._2));
            this.hBoxes.get(i / this.col).getChildren().add(this.imageViews.get(i));
            if (t.isLocal()) {
                return this.loadImageFromLocal(t.getLocalPath(), this.imgWidth.get(), this.imgHeight.get(), this.imageViews.get(i));
            } else {
                return this.loadImageFromURL(t.getThumbURL(), this.imgWidth.get(), this.imgHeight.get(), this.imageViews.get(i));
            }
        });

        Flux.fromIterable(task)
                .parallel()
                .runOn(FxThumb.getAsyncScheduler())
                .doOnNext(Mono::subscribe)
                .subscribe();
    }

    @NotNull
    public Mono<Image> loadImageFromURL(URL url, double w, double h, ImageView view) {
        return FxThumb.monoLoadImageFromURLImp(url, w, h, FxThumb.DEFAULT_USER_AGENT, "", GUICommon.customReferrer(url, null))
                .doOnSuccess(i -> Platform.runLater(() -> {
                    view.setImage(i);
                    view.setVisible(true);
                }))
                .doOnError(e -> GUICommon.debugMessage(() -> ">> Error at loadImageFromURL : " + e.toString()));
    }

    @NotNull
    public Mono<Image> loadImageFromLocal(Path path, double w, double h, ImageView view) {
        return FxThumb.monoLoadImageFromPathImp(path, w, h)
                .doOnSuccess(i -> Platform.runLater(() -> {
                    view.setImage(i);
                    view.setVisible(true);
                }))
                .doOnError(e -> GUICommon.debugMessage(() -> ">> Error at loadImageFromLocal : " + e.toString()));
    }
}
