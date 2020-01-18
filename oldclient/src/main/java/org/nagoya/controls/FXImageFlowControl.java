package org.nagoya.controls;

import com.jfoenix.controls.JFXButton;
import io.vavr.collection.Vector;
import javafx.animation.FadeTransition;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.view.FXImageObservableWrapper;

import java.util.Objects;
import java.util.function.Consumer;

public class FXImageFlowControl extends FXResizeableVBox {
    private final ObjectProperty<Image> mainImage = new SimpleObjectProperty<>();
    private final ObjectProperty<Image> prevImage = new SimpleObjectProperty<>();
    private final ObjectProperty<Image> nextImage = new SimpleObjectProperty<>();
    private final IntegerProperty integerProperty = new SimpleIntegerProperty();
    private final StringProperty lenProperty = new SimpleStringProperty();

    private final ObjectProperty<Vector<FxThumb>> imageList;

    private HBox hBoxControl;

    @NotNull
    public static FXImageFlowControl create() {
        return new FXImageFlowControl();
    }

    public FXImageFlowControl() {
        this.imageList = new SimpleObjectProperty<>(Vector.empty()) {
            @Override
            protected void invalidated() {
                FXImageFlowControl.this.lenProperty.setValue(" / " + FXImageFlowControl.this.imageList.get().length());
            }
        };

        this.init();
    }

    private void init() {
        this.setSpacing(0);
        this.setPadding(FXWindow.getDefaultInset());

        HBox hBoxImg = GUICommon.hbox(0);
        hBoxImg.setPadding(Insets.EMPTY);

        this.hBoxControl = GUICommon.hbox(10);
        this.hBoxControl.setAlignment(Pos.CENTER);
        this.hBoxControl.setMinHeight(USE_PREF_SIZE);
        this.hBoxControl.setPrefHeight(60);
        this.hBoxControl.setMaxHeight(USE_PREF_SIZE);

        ImageView viewBig = new ImageView();
        ImageView viewPrev = new ImageView();
        ImageView viewNext = new ImageView();

        viewBig.setPreserveRatio(true);
        viewPrev.setPreserveRatio(true);
        viewNext.setPreserveRatio(true);

        viewPrev.fitWidthProperty().bind(this.areaWidthProperty().divide(4));
        viewPrev.fitHeightProperty().bind(this.areaHeightProperty().divide(1.5));
        viewNext.fitWidthProperty().bind(this.areaWidthProperty().divide(4));
        viewNext.fitHeightProperty().bind(this.areaHeightProperty().divide(1.5));

        viewPrev.setOpacity(0.8);
        viewNext.setOpacity(0.8);

        StackPane leftPane = new StackPane();
        StackPane rightPane = new StackPane();

        leftPane.setAlignment(Pos.CENTER_RIGHT);
        rightPane.setAlignment(Pos.CENTER_LEFT);
        leftPane.setPadding(Insets.EMPTY);
        rightPane.setPadding(Insets.EMPTY);

        leftPane.prefWidthProperty().bind(viewPrev.fitWidthProperty());
        leftPane.prefHeightProperty().bind(viewPrev.fitHeightProperty());
        rightPane.prefWidthProperty().bind(viewNext.fitWidthProperty());
        rightPane.prefHeightProperty().bind(viewNext.fitHeightProperty());
        leftPane.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        rightPane.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);

        leftPane.getChildren().add(viewPrev);
        rightPane.getChildren().add(viewNext);

        FXZoomableScrollPane midPane = new FXZoomableScrollPane(viewBig);
        midPane.setPadding(Insets.EMPTY);
        midPane.prefWidthProperty().bind(this.areaWidthProperty().divide(2));
        midPane.prefHeightProperty().bind(this.areaHeightProperty().subtract(this.hBoxControl.heightProperty()));
        midPane.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);

        this.mainImage.addListener((ov, o, n) -> {
            if (Objects.nonNull(n)) {
                midPane.contentUpdated(n.getWidth(), n.getHeight());
            }
        });

        //midPane.widthProperty().addListener((ov, o, n)->(ChangeListener)midPane::sizeUpdated);

        Text idxTxt = new Text();
        JFXButton titleTxt = FXFactory.buttonWithBorder("", null);
        titleTxt.setMinWidth(400);
        titleTxt.setDisable(true);
        this.integerProperty.setValue(-1);

        FadeTransition fadeEffect = new FadeTransition(Duration.millis(500), hBoxImg);
        fadeEffect.setFromValue(0);
        fadeEffect.setToValue(1);
        fadeEffect.setAutoReverse(true);

        FXImageObservableWrapper viewWrapperLeft = new FXImageObservableWrapper(this.prevImage);
        FXImageObservableWrapper viewWrapperMid = new FXImageObservableWrapper(this.mainImage);
        FXImageObservableWrapper viewWrapperRight = new FXImageObservableWrapper(this.nextImage);

        viewWrapperMid.customProcessor((image, imageProperty) -> GUICommon.runOnFx(() -> {
            imageProperty.setValue(image);
            fadeEffect.play();
        }));

        JFXButton bNext = FXFactory.buttonWithBorder("  >  ", (e2) -> {
            if (this.integerProperty.get() < this.imageList.get().length() - 1) {
                this.integerProperty.setValue(this.integerProperty.get() + 1);
            }
        });

        JFXButton bPrev = FXFactory.buttonWithBorder("  <  ", (e1) -> {
            if (this.integerProperty.get() > 0) {
                this.integerProperty.setValue(this.integerProperty.get() - 1);
            }
        });

        Consumer<Integer> showImage = (i) ->
        {
            viewWrapperLeft.setKey(i);
            viewWrapperMid.setKey(i);
            viewWrapperRight.setKey(i);

            FxThumb thumb = this.imageList.get().get(i);
            titleTxt.setText(thumb.getThumbLabel());
            thumb.getImage(viewWrapperMid::setImage, i);

            if (i > 0) {
                this.imageList.get().get(i - 1).getImage(viewWrapperLeft::setImage, i);
            } else {
                this.prevImage.setValue(FxThumb.EMPTY_IMAGE);
            }

            if (i < this.imageList.get().length() - 1) {
                this.imageList.get().get(i + 1).getImage(viewWrapperRight::setImage, i);
            } else {
                this.nextImage.setValue(FxThumb.EMPTY_IMAGE);
            }
        };

        viewBig.imageProperty().bind(this.mainImage);
        viewPrev.imageProperty().bind(this.prevImage);
        viewNext.imageProperty().bind(this.nextImage);
        idxTxt.textProperty().bind(this.integerProperty.add(1).asString().concat(this.lenProperty));
        this.integerProperty.addListener((ov, o, n) -> showImage.accept(n.intValue()));

        this.hBoxControl.getChildren().addAll(titleTxt, bPrev, idxTxt, bNext);
        hBoxImg.getChildren().setAll(leftPane, midPane, rightPane);

        this.getChildren().addAll(this.hBoxControl, hBoxImg);

    }

    @Override
    protected double computeMinWidth(double height) {
        double min = this.snapSizeX(Math.max(400, this.hBoxControl.minWidth(-1) + this.snappedLeftInset() + this.snappedRightInset()));
        this.setStageMinWidth(min);
        return min;
    }

    @Override
    protected double computeMinHeight(double width) {
        double min = 300;
        this.setStageMinHeight(min);
        return min;
    }

    public void setItem(@NotNull Vector<FxThumb> imageList, int index) {
        this.imageList.setValue(imageList);
        this.integerProperty.setValue(index);
    }
}
