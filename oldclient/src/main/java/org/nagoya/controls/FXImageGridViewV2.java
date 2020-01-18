package org.nagoya.controls;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Vector;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.nagoya.view.FXImageViewWrapper;

import java.util.Random;

public class FXImageGridViewV2 extends GridPane {
    private final int MAX_COL = 20;
    private final int MAX_ROW = 20;
    private final int MAX_IMAGE = this.MAX_COL * this.MAX_ROW;

    private final IntegerProperty colCount;
    private final IntegerProperty rowCount;
    private final DoubleProperty spaceWidth;
    private final DoubleProperty spaceHeight;

    private final Random random = new Random();
    private final Vector<Tuple2<Pane, ImageView>> imageViews;
    private final FXImageGridControl imageControl;
    private final FXImageViewWrapper imageWrapper = new FXImageViewWrapper();

    private final DoubleBinding bindW;
    private final DoubleBinding bindH;


    FXImageGridViewV2(@NotNull FXImageGridControl control) {
        this.imageControl = control;

        this.colCount = new SimpleIntegerProperty(1) {
            @Override
            protected void invalidated() {
                if (this.get() < 1) {
                    this.set(1);
                } else if (this.get() > FXImageGridViewV2.this.MAX_COL) {
                    this.set(FXImageGridViewV2.this.MAX_COL);
                }
            }
        };

        this.rowCount = new SimpleIntegerProperty(1) {
            @Override
            protected void invalidated() {
                if (this.get() < 1) {
                    this.set(1);
                } else if (this.get() > FXImageGridViewV2.this.MAX_ROW) {
                    this.set(FXImageGridViewV2.this.MAX_ROW);
                }
            }
        };

        this.spaceWidth = new SimpleDoubleProperty(1);
        this.spaceHeight = new SimpleDoubleProperty(1);

        this.spaceWidth.bind(Bindings.createDoubleBinding(this::computeSpaceWidth, this.widthProperty(), this.colCount, this.insetsProperty()));
        this.spaceHeight.bind(Bindings.createDoubleBinding(this::computeSpaceHeight, this.heightProperty(), this.rowCount, this.insetsProperty()));

        this.setAlignment(Pos.CENTER);
        this.setVgap(0);
        this.setHgap(0);
        this.getStyleClass().add("image_grid_view");

        this.bindW = this.spaceWidth.divide(this.colCount);
        this.bindH = this.spaceHeight.divide(this.rowCount);

        this.imageViews = Vector.range(0, control.getImagePerPage()).map(i -> {
            StackPane pane = new StackPane();
            pane.setAlignment(Pos.CENTER);
            pane.getStyleClass().add("image_pane");
            pane.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
            pane.prefWidthProperty().bind(this.bindW);
            pane.prefHeightProperty().bind(this.bindH);

            ImageView view = new ImageView();
            view.setPreserveRatio(true);
            view.getStyleClass().add("image_view");
            view.fitWidthProperty().bind(this.bindW);
            view.fitHeightProperty().bind(this.bindH);

            pane.getChildren().addAll(view);
            return Tuple.of(pane, view);
        });
    }

    protected double computeSpaceWidth() {
        double w = this.snapSizeX(this.getWidth()) - this.snappedLeftInset() - this.snappedRightInset() - (this.getHgap() * this.colCount.get());
        //GUICommon.debugMessage("computeSpaceWidth " + w);
        return w;
    }

    protected double computeSpaceHeight() {
        double h = this.snapSizeY(this.getHeight()) - this.snappedTopInset() - this.snappedBottomInset() - (this.getVgap() * this.rowCount.get());
        //GUICommon.debugMessage("computeSpaceHeight " + h);
        return h;
    }

    protected void clearViews() {
        this.getChildren().clear();
        this.imageViews.forEach((view) -> view._2.setImage(null));
    }

    public void setItem(@NotNull Vector<Tuple2<FXScreenImage, Integer>> imageList, int vCol, int vRow) {
        this.clearViews();

        this.setSpaceCol(vCol);
        this.setSpaceRow(vRow);

        int randomKey = this.random.nextInt(50000);
        this.imageWrapper.setKey(randomKey);

        Vector<FXScreenImage> items = imageList.map(Tuple2::_1);

        if (items.length() > this.MAX_IMAGE) {
            items = items.take(this.MAX_IMAGE);
        }

        //GUICommon.debugMessage("Grid prefWidth" + this.prefWidth(-1));
        //GUICommon.debugMessage("Grid prefHeight" + this.prefHeight(-1));

        items.forEachWithIndex((t, i) -> {
            var view = this.imageViews.get(i)._2;
            view.setOnMouseClicked((e) -> this.imageControl.getOnImageClick().accept(imageList.get(i)._2));
            t.setMaxSize(view.getFitWidth(), view.getFitHeight());
            t.getImage((image) -> this.imageWrapper.setImage(image, randomKey, view));
            this.add(this.imageViews.get(i)._1, i % this.getSpaceCol(), i / this.getSpaceCol());
        });
    }

    public int getSpaceCol() {
        return this.colCount.get();
    }

    public IntegerProperty colCountProperty() {
        return this.colCount;
    }

    public void setSpaceCol(int colCount) {
        this.colCount.set(colCount);
    }

    public int getSpaceRow() {
        return this.rowCount.get();
    }

    public IntegerProperty rowCountProperty() {
        return this.rowCount;
    }

    public void setSpaceRow(int rowCount) {
        this.rowCount.set(rowCount);
    }

    public double getSpaceWidth() {
        return this.spaceWidth.get();
    }

    public DoubleProperty spaceWidthProperty() {
        return this.spaceWidth;
    }

    public void setSpaceWidth(double spaceWidth) {
        this.spaceWidth.set(spaceWidth);
    }

    public double getSpaceHeight() {
        return this.spaceHeight.get();
    }

    public DoubleProperty spaceHeightProperty() {
        return this.spaceHeight;
    }

    public void setSpaceHeight(double spaceHeight) {
        this.spaceHeight.set(spaceHeight);
    }
}
