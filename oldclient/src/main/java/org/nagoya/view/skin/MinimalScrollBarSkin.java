package org.nagoya.view.skin;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.DoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import org.controlsfx.tools.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MinimalScrollBarSkin extends Region implements Skin<ScrollBar> {
    private static final double DEFAULT_SCROLL_BAR_SIZE = 10;

    private ScrollBar scrollBar;
    private final NumberBinding range;
    private final NumberBinding position;
    private final Rectangle track = new Rectangle();
    private final Rectangle thumb = new Rectangle() {
        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute var1, Object... var2) {
            switch (var1) {
                case VALUE:
                    return MinimalScrollBarSkin.this.getSkinnable().getValue();
                default:
                    return super.queryAccessibleAttribute(var1, var2);
            }
        }
    };

    private double preDragThumbPos;
    private Point2D dragStart;
    private DoubleProperty scrollBarSize;

    public MinimalScrollBarSkin(@NotNull ScrollBar scrollBar) {
        this.scrollBar = scrollBar;

        this.range = Bindings.subtract(this.scrollBar.maxProperty(), this.scrollBar.minProperty());
        this.position = Bindings.divide(Bindings.subtract(this.scrollBar.valueProperty(), this.scrollBar.minProperty()), this.range);

        this.getStyleClass().add("scroll-bar");
        // Children are added unmanaged because for some reason the height of the bar keeps changing
        // if they're managed in certain situations... not sure about the cause.
        this.getChildren().addAll(this.track, this.thumb);

        this.track.setManaged(false);
        this.track.getStyleClass().add("track");

        this.thumb.setManaged(false);
        this.thumb.getStyleClass().add("thumb");
        this.thumb.setAccessibleRole(AccessibleRole.THUMB);

        scrollBar.orientationProperty().addListener(obs -> this.setup());

        this.thumb.setOnMousePressed((mouseEvent) -> {
            if (mouseEvent.isSynthesized()) {
                mouseEvent.consume();
            } else {
                if (this.getSkinnable().getMax() > this.getSkinnable().getMin()) {
                    this.dragStart = this.thumb.localToParent(mouseEvent.getX(), mouseEvent.getY());
                    double value = Utils.clamp(this.getSkinnable().getMin(), this.getSkinnable().getValue(), this.getSkinnable().getMax());
                    this.preDragThumbPos = (value - this.getSkinnable().getMin()) / (this.getSkinnable().getMax() - this.getSkinnable().getMin());
                    mouseEvent.consume();
                }

            }
        });

        this.thumb.setOnMouseDragged((mouseEvent) -> {
            if (mouseEvent.isSynthesized()) {
                mouseEvent.consume();
            } else {
                if (this.getSkinnable().getMax() > this.getSkinnable().getMin()) {
                    if (this.trackLength() > this.thumbLength()) {
                        Point2D pos = this.thumb.localToParent(mouseEvent.getX(), mouseEvent.getY());
                        if (this.dragStart == null) {
                            this.dragStart = this.thumb.localToParent(mouseEvent.getX(), mouseEvent.getY());
                        }
                        double value = this.getSkinnable().getOrientation() == Orientation.VERTICAL ? pos.getY() - this.dragStart.getY() : pos.getX() - this.dragStart.getX();
                        double change = this.preDragThumbPos + value / (this.trackLength() - this.thumbLength());
                        double newValue = change * (this.getSkinnable().getMax() - this.getSkinnable().getMin()) + this.getSkinnable().getMin();
                        if (!Double.isNaN(newValue)) {
                            this.getSkinnable().setValue(Utils.clamp(this.getSkinnable().getMin(), newValue, this.getSkinnable().getMax()));
                        }
                    }
                    mouseEvent.consume();
                }
            }
        });
        this.setup();
    }

    private double trackLength() {
        return this.getSkinnable().getOrientation() == Orientation.VERTICAL ? this.track.getHeight() : this.track.getWidth();
    }

    private double thumbLength() {
        return this.getSkinnable().getOrientation() == Orientation.VERTICAL ? this.thumb.getHeight() : this.thumb.getWidth();
    }

    private void setup() {
        //GUICommon.debugMessage("scroll skin setup " + this.getScrollBarWide());

        double size = this.getScrollBarSize();

        this.track.widthProperty().unbind();
        this.track.heightProperty().unbind();

        if (this.scrollBar.getOrientation() == Orientation.HORIZONTAL) {
            this.track.relocate(0, -size);
            this.track.widthProperty().bind(this.scrollBar.widthProperty());
            this.track.setHeight(size);
            this.getSkinnable().setPrefHeight(this.track.getStrokeWidth());
        } else {
            this.track.relocate(-size, 0);
            this.track.setWidth(size);
            this.track.heightProperty().bind(this.scrollBar.heightProperty());
            this.getSkinnable().setPrefWidth(this.track.getStrokeWidth());
        }

        this.thumb.xProperty().unbind();
        this.thumb.yProperty().unbind();
        this.thumb.widthProperty().unbind();
        this.thumb.heightProperty().unbind();

        if (this.scrollBar.getOrientation() == Orientation.HORIZONTAL) {
            this.thumb.relocate(0, -size);
            this.thumb.widthProperty().bind(Bindings.max(size, this.scrollBar.visibleAmountProperty().divide(this.range).multiply(this.scrollBar.widthProperty())));
            this.thumb.setHeight(size);
            this.thumb.xProperty().bind(Bindings.subtract(this.scrollBar.widthProperty(), this.thumb.widthProperty()).multiply(this.position));
        } else {
            this.thumb.relocate(-size, 0);
            this.thumb.setWidth(size);
            this.thumb.heightProperty().bind(Bindings.max(size, this.scrollBar.visibleAmountProperty().divide(this.range).multiply(this.scrollBar.heightProperty())));
            this.thumb.yProperty().bind(Bindings.subtract(this.scrollBar.heightProperty(), this.thumb.heightProperty()).multiply(this.position));
        }
    }

    private double getScrollBarSize() {
        return this.scrollBarSize == null ? DEFAULT_SCROLL_BAR_SIZE : this.scrollBarSize.get();
    }

    private DoubleProperty scrollBarSizeProperty() {
        if (this.scrollBarSize == null) {
            this.scrollBarSize = new StyleableDoubleProperty(DEFAULT_SCROLL_BAR_SIZE) {
                @Override
                protected void invalidated() {
                    MinimalScrollBarSkin.this.setup();
                }

                @Override
                public CssMetaData<MinimalScrollBarSkin, Number> getCssMetaData() {
                    return StyleableProperties.SCROLL_BAR_SIZE;
                }

                @Override
                public Object getBean() {
                    return MinimalScrollBarSkin.this;
                }

                @Override
                public String getName() {
                    return "scrollBarSize";
                }
            };
        }
        return this.scrollBarSize;
    }

    @Override
    protected double computeMaxWidth(double height) {
        if (this.scrollBar.getOrientation() == Orientation.HORIZONTAL) {
            return Double.MAX_VALUE;
        }
        return this.getScrollBarSize();
    }

    @Override
    protected double computeMaxHeight(double width) {
        if (this.scrollBar.getOrientation() == Orientation.VERTICAL) {
            return Double.MAX_VALUE;
        }
        return this.getScrollBarSize();
    }

    @Override
    public void dispose() {
        this.scrollBar = null;
    }

    @Override
    public Node getNode() {
        return this;
    }

    @Override
    public ScrollBar getSkinnable() {
        return this.scrollBar;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return MinimalScrollBarSkin.getClassCssMetaData();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return MinimalScrollBarSkin.StyleableProperties.STYLEABLES;
    }

    private static class StyleableProperties {
        private static final CssMetaData<MinimalScrollBarSkin, Number> SCROLL_BAR_SIZE;
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        private StyleableProperties() {
        }

        static {
            SCROLL_BAR_SIZE = new CssMetaData<>("-fx-scroll-bar-size", SizeConverter.getInstance(), 10) {
                @Override
                public boolean isSettable(MinimalScrollBarSkin skin) {
                    return skin.scrollBarSize == null || !skin.scrollBarSize.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(MinimalScrollBarSkin skin) {
                    return (StyleableProperty) skin.scrollBarSizeProperty();
                }
            };

            ArrayList<CssMetaData<? extends Styleable, ?>> cssMetaData = new ArrayList<>(Region.getClassCssMetaData());
            cssMetaData.add(SCROLL_BAR_SIZE);
            STYLEABLES = Collections.unmodifiableList(cssMetaData);
        }
    }
}