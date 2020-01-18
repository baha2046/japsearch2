package org.nagoya.view.skin;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.css.*;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.SizeConverter;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.nagoya.GUICommon;
import org.nagoya.controls.FXPagination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FXPaginationSkin extends SkinBase<FXPagination> {
    private final FXPagination pagination;

    private final FXPaginationSkin.NavigationControl navigation;
    private final Rectangle clipRect;
    private int fromIndex;
    private int previousIndex;
    private int currentIndex;
    private int toIndex;
    private int pageCount;
    private int maxPageIndicatorCount;

    private BooleanProperty arrowsVisible;
    private BooleanProperty pageInformationVisible;
    private static final Boolean DEFAULT_ARROW_VISIBLE;
    private static final Boolean DEFAULT_PAGE_INFORMATION_VISIBLE;

    public FXPaginationSkin(FXPagination fxPagination) {
        super(fxPagination);
        this.pagination = fxPagination;

        this.clipRect = new Rectangle();
        this.getSkinnable().setClip(this.clipRect);

        this.navigation = new NavigationControl();
        this.getChildren().addAll(this.navigation);

        fxPagination.maxPageIndicatorCountProperty().addListener((ov, o, n) -> {
            GUICommon.debugMessage("maxPageIndicatorCountProperty change " + n);
            if (!o.equals(n)) {
                this.resetIndiciesAndNav();
            }
        });
        this.registerChangeListener(fxPagination.widthProperty(), (var1x) -> {
            this.clipRect.setWidth((this.getSkinnable()).getWidth());
        });
        this.registerChangeListener(fxPagination.heightProperty(), (var1x) -> {
            this.clipRect.setHeight((this.getSkinnable()).getHeight());
        });
        this.registerChangeListener(fxPagination.pageCountProperty(), (var1x) -> {
            GUICommon.debugMessage("fxPagination.pageCountProperty change");
            this.resetIndiciesAndNav();
        });
        this.registerChangeListener(fxPagination.pageFactoryProperty(), (var1x) -> {
            this.resetIndiciesAndNav();
        });
    }


    private final DoubleProperty arrowButtonGap = new StyleableDoubleProperty(60.0D) {
        @Override
        public Object getBean() {
            return FXPaginationSkin.this;
        }

        @Override
        public String getName() {
            return "arrowButtonGap";
        }

        @Override
        public CssMetaData<FXPagination, Number> getCssMetaData() {
            return FXPaginationSkin.StyleableProperties.ARROW_BUTTON_GAP;
        }
    };

    private final DoubleProperty arrowButtonGapProperty() {
        return this.arrowButtonGap;
    }

    private final double getArrowButtonGap() {
        return this.arrowButtonGap.get();
    }

    private final void setArrowButtonGap(double var1) {
        this.arrowButtonGap.set(var1);
    }

    private final void setArrowsVisible(boolean var1) {
        this.arrowsVisibleProperty().set(var1);
    }

    private final boolean isArrowsVisible() {
        return this.arrowsVisible == null ? DEFAULT_ARROW_VISIBLE : this.arrowsVisible.get();
    }

    private final BooleanProperty arrowsVisibleProperty() {
        if (this.arrowsVisible == null) {
            this.arrowsVisible = new StyleableBooleanProperty(DEFAULT_ARROW_VISIBLE) {
                @Override
                protected void invalidated() {
                    FXPaginationSkin.this.getSkinnable().requestLayout();
                }

                @Override
                public CssMetaData<FXPagination, Boolean> getCssMetaData() {
                    return FXPaginationSkin.StyleableProperties.ARROWS_VISIBLE;
                }

                @Override
                public Object getBean() {
                    return FXPaginationSkin.this;
                }

                @Override
                public String getName() {
                    return "arrowVisible";
                }
            };
        }

        return this.arrowsVisible;
    }

    private final void setPageInformationVisible(boolean var1) {
        this.pageInformationVisibleProperty().set(var1);
    }

    private final boolean isPageInformationVisible() {
        return this.pageInformationVisible == null ? DEFAULT_PAGE_INFORMATION_VISIBLE : this.pageInformationVisible.get();
    }

    private final BooleanProperty pageInformationVisibleProperty() {
        if (this.pageInformationVisible == null) {
            this.pageInformationVisible = new StyleableBooleanProperty(DEFAULT_PAGE_INFORMATION_VISIBLE) {
                @Override
                protected void invalidated() {
                    (FXPaginationSkin.this.getSkinnable()).requestLayout();
                }

                @Override
                public CssMetaData<FXPagination, Boolean> getCssMetaData() {
                    return FXPaginationSkin.StyleableProperties.PAGE_INFORMATION_VISIBLE;
                }

                @Override
                public Object getBean() {
                    return FXPaginationSkin.this;
                }

                @Override
                public String getName() {
                    return "pageInformationVisible";
                }
            };
        }
        return this.pageInformationVisible;
    }

    @Override
    public void dispose() {
        super.dispose();

    }

    //double height, double topInset, double rightInset, double bottomInset, double leftInset
    @Override
    protected double computeMinWidth(double var1, double var3, double var5, double var7, double var9) {
        double var11 = this.navigation.isVisible() ? this.snapSizeX(this.navigation.minWidth(var1)) : 0.0D;
        return var9 + var11 + var5;
    }

    @Override
    protected double computeMinHeight(double var1, double var3, double var5, double var7, double var9) {
        double var11 = this.navigation.isVisible() ? this.snapSizeY(this.navigation.minHeight(var1)) : 0.0D;
        return var3 + var11 + var7;
    }

    @Override
    protected double computePrefWidth(double var1, double var3, double var5, double var7, double var9) {
        double var11 = this.navigation.isVisible() ? this.snapSizeX(this.navigation.prefWidth(var1)) : 0.0D;
        return var9 + var11 + var5;
    }

    @Override
    protected double computePrefHeight(double var1, double var3, double var5, double var7, double var9) {
        double var11 = this.navigation.isVisible() ? this.snapSizeY(this.navigation.prefHeight(var1)) : 0.0D;
        return var3 + var11 + var7;
    }

    //(double contentX, double contentY, double contentWidth, double contentHeight)
    @Override
    protected void layoutChildren(double var1, double var3, double var5, double var7) {
        this.layoutInArea(this.navigation, var1, var3, var5, var7, 0.0D, HPos.CENTER, VPos.CENTER);
    }

    @Override
    protected Object queryAccessibleAttribute(AccessibleAttribute var1, Object... var2) {
        switch (var1) {
            case FOCUS_ITEM:
                return this.navigation.indicatorButtons.getSelectedToggle();
            case ITEM_COUNT:
                return this.navigation.indicatorButtons.getToggles().size();
            case ITEM_AT_INDEX:
                Integer idx = (Integer) var2[0];
                if (idx == null) {
                    return null;
                }

                return this.navigation.indicatorButtons.getToggles().get(idx);
            default:
                return super.queryAccessibleAttribute(var1, var2);
        }
    }

    private void selectNext() {
        if (this.getCurrentPageIndex() < this.getPageCount() - 1) {
            this.pagination.setCurrentPageIndex(this.getCurrentPageIndex() + 1);
        }

    }

    private void selectPrevious() {
        if (this.getCurrentPageIndex() > 0) {
            this.pagination.setCurrentPageIndex(this.getCurrentPageIndex() - 1);
        }

    }

    private void resetIndiciesAndNav() {
        this.resetIndexes();
        this.navigation.initializePageIndicators();
        this.navigation.updatePageIndicators();
    }

    private void resetIndexes() {
        this.maxPageIndicatorCount = this.getMaxPageIndicatorCount();
        this.pageCount = this.getPageCount();
        if (this.pageCount > this.maxPageIndicatorCount) {
            this.pageCount = this.maxPageIndicatorCount;
        }

        this.fromIndex = 0;
        this.previousIndex = 0;
        this.toIndex = this.pageCount - 1;
        this.currentIndex = this.getCurrentPageIndex();/*Math.min(this.getCurrentPageIndex(), this.toIndex);*///var1 ? this.getCurrentPageIndex() : 0;
        if (this.pageCount == 2147483647 && this.maxPageIndicatorCount == 2147483647) {
            this.toIndex = 0;
        }

        //GUICommon.debugMessage("SKIN SET currentPageIndex " + this.currentIndex);
        this.pagination.setCurrentPageIndex(this.currentIndex);
        this.createPage(this.currentIndex);
    }

    private void createPage(int pageNum) {
        if (this.pagination.getPageFactory() != null) {
            this.pagination.getPageFactory().accept(pageNum);
        }
    }

    private int getPageCount() {
        return Math.max(this.getSkinnable().getPageCount(), 1);
    }

    private int getMaxPageIndicatorCount() {
        return this.getSkinnable().getMaxPageIndicatorCount();
    }

    private int getCurrentPageIndex() {
        return this.getSkinnable().getCurrentPageIndex();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return FXPaginationSkin.StyleableProperties.STYLEABLES;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    static {
        DEFAULT_ARROW_VISIBLE = Boolean.FALSE;
        DEFAULT_PAGE_INFORMATION_VISIBLE = Boolean.FALSE;
    }

    private static class StyleableProperties {
        private static final CssMetaData<FXPagination, Boolean> ARROWS_VISIBLE;
        private static final CssMetaData<FXPagination, Boolean> PAGE_INFORMATION_VISIBLE;
        private static final CssMetaData<FXPagination, Number> ARROW_BUTTON_GAP;
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        private StyleableProperties() {
        }

        static {
            ARROWS_VISIBLE = new CssMetaData<>("-fx-arrows-visible", BooleanConverter.getInstance(), FXPaginationSkin.DEFAULT_ARROW_VISIBLE) {
                @Override
                public boolean isSettable(FXPagination var1) {
                    FXPaginationSkin var2 = (FXPaginationSkin) var1.getSkin();
                    return var2.arrowsVisible == null || !var2.arrowsVisible.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(FXPagination var1) {
                    FXPaginationSkin var2 = (FXPaginationSkin) var1.getSkin();
                    return (StyleableProperty) var2.arrowsVisibleProperty();
                }
            };
            PAGE_INFORMATION_VISIBLE = new CssMetaData<>("-fx-page-information-visible", BooleanConverter.getInstance(), FXPaginationSkin.DEFAULT_PAGE_INFORMATION_VISIBLE) {
                @Override
                public boolean isSettable(FXPagination var1) {
                    FXPaginationSkin var2 = (FXPaginationSkin) var1.getSkin();
                    return var2.pageInformationVisible == null || !var2.pageInformationVisible.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(FXPagination var1) {
                    FXPaginationSkin var2 = (FXPaginationSkin) var1.getSkin();
                    return (StyleableProperty) var2.pageInformationVisibleProperty();
                }
            };
            ARROW_BUTTON_GAP = new CssMetaData<>("-fx-arrow-button-gap", SizeConverter.getInstance(), 4) {
                @Override
                public boolean isSettable(FXPagination var1) {
                    FXPaginationSkin var2 = (FXPaginationSkin) var1.getSkin();
                    return var2.arrowButtonGap == null || !var2.arrowButtonGap.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(FXPagination var1) {
                    FXPaginationSkin var2 = (FXPaginationSkin) var1.getSkin();
                    return (StyleableProperty) var2.arrowButtonGapProperty();
                }
            };
            ArrayList<CssMetaData<? extends Styleable, ?>> cssMetaData = new ArrayList<>(SkinBase.getClassCssMetaData());
            cssMetaData.add(ARROWS_VISIBLE);
            cssMetaData.add(PAGE_INFORMATION_VISIBLE);
            cssMetaData.add(ARROW_BUTTON_GAP);
            STYLEABLES = Collections.unmodifiableList(cssMetaData);
        }
    }

    class NavigationControl extends StackPane {
        private final HBox controlBox;
        private final Button leftArrowButton;
        private final StackPane leftArrow;
        private final Button rightArrowButton;
        private final StackPane rightArrow;
        private final ToggleGroup indicatorButtons;
        private final Label pageInformation;
        private double minButtonSize = -1.0D;
        private int previousIndicatorCount = 0;

        public NavigationControl() {
            this.getStyleClass().setAll("pagination-control");

            this.controlBox = new HBox();
            this.controlBox.getStyleClass().add("control-box");
            this.leftArrowButton = new Button();
            this.leftArrowButton.setAccessibleText("Previous Button");
            this.minButtonSize = this.leftArrowButton.getFont().getSize() * 2.0D;
            this.leftArrowButton.fontProperty().addListener((var1x, var2, var3) -> {
                this.minButtonSize = var3.getSize() * 2.0D;
                for (Node node : this.controlBox.getChildren()) {
                    ((Control) node).setMinSize(this.minButtonSize, this.minButtonSize);
                }
                this.requestLayout();
            });
            this.leftArrowButton.setMinSize(this.minButtonSize, this.minButtonSize);
            this.leftArrowButton.prefWidthProperty().bind(this.leftArrowButton.minWidthProperty());
            this.leftArrowButton.prefHeightProperty().bind(this.leftArrowButton.minHeightProperty());
            this.leftArrowButton.getStyleClass().add("left-arrow-button");
            this.leftArrowButton.setFocusTraversable(false);
            HBox.setMargin(this.leftArrowButton, new Insets(0.0D, this.snapSizeX(FXPaginationSkin.this.arrowButtonGap.get()), 0.0D, 0.0D));
            this.leftArrow = new StackPane();
            this.leftArrow.setMaxSize(-1.0D, -1.0D);
            this.leftArrowButton.setGraphic(this.leftArrow);
            this.leftArrow.getStyleClass().add("left-arrow");
            this.rightArrowButton = new Button();
            this.rightArrowButton.setAccessibleText("Next Button");
            this.rightArrowButton.setMinSize(this.minButtonSize, this.minButtonSize);
            this.rightArrowButton.prefWidthProperty().bind(this.rightArrowButton.minWidthProperty());
            this.rightArrowButton.prefHeightProperty().bind(this.rightArrowButton.minHeightProperty());
            this.rightArrowButton.getStyleClass().add("right-arrow-button");
            this.rightArrowButton.setFocusTraversable(false);
            HBox.setMargin(this.rightArrowButton, new Insets(0.0D, 0.0D, 0.0D, this.snapSizeX(FXPaginationSkin.this.arrowButtonGap.get())));
            this.rightArrow = new StackPane();
            this.rightArrow.setMaxSize(-1.0D, -1.0D);
            this.rightArrowButton.setGraphic(this.rightArrow);
            this.rightArrow.getStyleClass().add("right-arrow");
            this.indicatorButtons = new ToggleGroup();
            this.pageInformation = new Label();
            this.pageInformation.getStyleClass().add("page-information");
            this.pageInformation.setMinHeight(this.pageInformation.getFont().getSize() * 2);
            this.pageInformation.prefHeightProperty().bind(this.pageInformation.minHeightProperty());
            this.getChildren().addAll(this.controlBox, this.pageInformation);
            this.initializeNavigationHandlers();
            this.initializePageIndicators();
            this.updatePageIndex();
            FXPaginationSkin.this.arrowButtonGap.addListener((ov, o, n) -> {
                if (n.doubleValue() == 0.0D) {
                    HBox.setMargin(this.leftArrowButton, (Insets) null);
                    HBox.setMargin(this.rightArrowButton, (Insets) null);
                } else {
                    HBox.setMargin(this.leftArrowButton, new Insets(0.0D, this.snapSizeX(n.doubleValue()), 0.0D, 0.0D));
                    HBox.setMargin(this.rightArrowButton, new Insets(0.0D, 0.0D, 0.0D, this.snapSizeX(n.doubleValue())));
                }
            });
        }

        private void initializeNavigationHandlers() {
            this.leftArrowButton.setOnAction((var1) -> {
                FXPaginationSkin.this.getNode().requestFocus();
                FXPaginationSkin.this.selectPrevious();
                this.requestLayout();
            });
            this.rightArrowButton.setOnAction((var1) -> {
                FXPaginationSkin.this.getNode().requestFocus();
                FXPaginationSkin.this.selectNext();
                this.requestLayout();
            });
            FXPaginationSkin.this.pagination.currentPageIndexProperty().addListener((ov, o, n) -> {
                FXPaginationSkin.this.previousIndex = o.intValue();
                FXPaginationSkin.this.currentIndex = n.intValue();
                this.updatePageIndex();
                FXPaginationSkin.this.createPage(FXPaginationSkin.this.currentIndex);
            });
        }

        private void initializePageIndicators() {
            this.previousIndicatorCount = 0;
            this.controlBox.getChildren().clear();
            this.clearIndicatorButtons();
            this.controlBox.getChildren().add(this.leftArrowButton);
            for (int i = FXPaginationSkin.this.fromIndex; i <= FXPaginationSkin.this.toIndex; ++i) {
                FXPaginationSkin.IndicatorButton btn = FXPaginationSkin.this.new IndicatorButton(i);
                btn.setMinSize(this.minButtonSize, this.minButtonSize);
                btn.setToggleGroup(this.indicatorButtons);
                this.controlBox.getChildren().add(btn);
            }
            this.controlBox.getChildren().add(this.rightArrowButton);
        }

        private void clearIndicatorButtons() {
            for (Toggle btn : this.indicatorButtons.getToggles()) {
                if (btn instanceof FXPaginationSkin.IndicatorButton) {
                    FXPaginationSkin.IndicatorButton iBtn = (FXPaginationSkin.IndicatorButton) btn;
                    iBtn.release();
                }
            }
            this.indicatorButtons.getToggles().clear();
        }

        private void updatePageIndicators() {
            for (int i = 0; i < this.indicatorButtons.getToggles().size(); ++i) {
                FXPaginationSkin.IndicatorButton btn = (FXPaginationSkin.IndicatorButton) this.indicatorButtons.getToggles().get(i);
                if (btn.getPageNumber() == FXPaginationSkin.this.currentIndex) {
                    btn.setSelected(true);
                    this.updatePageInformation();
                    break;
                }
            }
            FXPaginationSkin.this.getSkinnable().notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_ITEM);
        }

        private void updatePageIndex() {
            if (FXPaginationSkin.this.pageCount == FXPaginationSkin.this.maxPageIndicatorCount && this.changePageSet()) {
                this.initializePageIndicators();
            }
            this.updatePageIndicators();
            this.requestLayout();
        }

        private void updatePageInformation() {
            String current = Integer.toString(FXPaginationSkin.this.currentIndex + 1);
            String total = FXPaginationSkin.this.getPageCount() == 2147483647 ? "..." : Integer.toString(FXPaginationSkin.this.getPageCount());
            this.pageInformation.setText(current + "/" + total);
        }

        private void layoutPageIndicators() {
            double var1 = this.snappedLeftInset();
            double var3 = this.snappedRightInset();
            double var5 = this.snapSizeX(this.getWidth()) - (var1 + var3);
            double var7 = this.controlBox.snappedLeftInset();
            double var9 = this.controlBox.snappedRightInset();
            double var11 = this.snapSizeX(Utils.boundedSize(this.leftArrowButton.prefWidth(-1.0D), this.leftArrowButton.minWidth(-1.0D), this.leftArrowButton.maxWidth(-1.0D)));
            double var13 = this.snapSizeX(Utils.boundedSize(this.rightArrowButton.prefWidth(-1.0D), this.rightArrowButton.minWidth(-1.0D), this.rightArrowButton.maxWidth(-1.0D)));
            double var15 = this.snapSizeX(this.controlBox.getSpacing());
            double var17 = var5 - (var7 + var11 + 2.0D * FXPaginationSkin.this.arrowButtonGap.get() + var15 + var13 + var9);

            double var19 = 0.0D;
            int iCount = 0;

            int var22;
            for (var22 = 0; var22 < FXPaginationSkin.this.getMaxPageIndicatorCount(); ++var22) {
                int var23 = var22 < this.indicatorButtons.getToggles().size() ? var22 : this.indicatorButtons.getToggles().size() - 1;
                double var24 = this.minButtonSize;
                if (var23 != -1) {
                    FXPaginationSkin.IndicatorButton var26 = (FXPaginationSkin.IndicatorButton) this.indicatorButtons.getToggles().get(var23);
                    var24 = this.snapSizeX(Utils.boundedSize(var26.prefWidth(-1.0D), var26.minWidth(-1.0D), var26.maxWidth(-1.0D)));
                }

                var19 += var24 + var15;
                if (var19 > var17) {
                    break;
                }

                ++iCount;
            }

            if (iCount == 0) {
                iCount = 1;
            }

            if (iCount != this.previousIndicatorCount) {
                FXPaginationSkin.this.maxPageIndicatorCount = Math.min(iCount, FXPaginationSkin.this.getMaxPageIndicatorCount());

                if (FXPaginationSkin.this.pageCount > FXPaginationSkin.this.maxPageIndicatorCount) {
                    FXPaginationSkin.this.pageCount = FXPaginationSkin.this.maxPageIndicatorCount;
                    var22 = FXPaginationSkin.this.maxPageIndicatorCount - 1;
                } else if (iCount > FXPaginationSkin.this.getPageCount()) {
                    FXPaginationSkin.this.pageCount = FXPaginationSkin.this.getPageCount();
                    var22 = FXPaginationSkin.this.getPageCount() - 1;
                } else {
                    FXPaginationSkin.this.pageCount = iCount;
                    var22 = iCount - 1;
                }

                if (FXPaginationSkin.this.currentIndex >= FXPaginationSkin.this.toIndex) {
                    FXPaginationSkin.this.toIndex = FXPaginationSkin.this.currentIndex;
                    FXPaginationSkin.this.fromIndex = FXPaginationSkin.this.toIndex - var22;
                } else if (FXPaginationSkin.this.currentIndex <= FXPaginationSkin.this.fromIndex) {
                    FXPaginationSkin.this.fromIndex = FXPaginationSkin.this.currentIndex;
                    FXPaginationSkin.this.toIndex = FXPaginationSkin.this.fromIndex + var22;
                } else {
                    FXPaginationSkin.this.toIndex = FXPaginationSkin.this.fromIndex + var22;
                }

                if (FXPaginationSkin.this.toIndex > FXPaginationSkin.this.getPageCount() - 1) {
                    FXPaginationSkin.this.toIndex = FXPaginationSkin.this.getPageCount() - 1;
                }

                if (FXPaginationSkin.this.fromIndex < 0) {
                    FXPaginationSkin.this.fromIndex = 0;
                    FXPaginationSkin.this.toIndex = FXPaginationSkin.this.fromIndex + var22;
                }

                this.initializePageIndicators();
                this.updatePageIndicators();
                this.previousIndicatorCount = iCount;
            }
        }

        private boolean changePageSet() {
            int var1 = this.indexToIndicatorButtonsIndex(FXPaginationSkin.this.currentIndex);
            int var2 = FXPaginationSkin.this.maxPageIndicatorCount - 1;
            if (FXPaginationSkin.this.previousIndex < FXPaginationSkin.this.currentIndex && var1 == 0 && var2 != 0 && var1 % var2 == 0) {
                FXPaginationSkin.this.fromIndex = FXPaginationSkin.this.currentIndex;
                FXPaginationSkin.this.toIndex = FXPaginationSkin.this.fromIndex + var2;
            } else if (FXPaginationSkin.this.currentIndex < FXPaginationSkin.this.previousIndex && var1 == var2 && var2 != 0 && var1 % var2 == 0) {
                FXPaginationSkin.this.toIndex = FXPaginationSkin.this.currentIndex;
                FXPaginationSkin.this.fromIndex = FXPaginationSkin.this.toIndex - var2;
            } else {
                if (FXPaginationSkin.this.currentIndex >= FXPaginationSkin.this.fromIndex && FXPaginationSkin.this.currentIndex <= FXPaginationSkin.this.toIndex) {
                    return false;
                }

                FXPaginationSkin.this.fromIndex = FXPaginationSkin.this.currentIndex - var1;
                FXPaginationSkin.this.toIndex = FXPaginationSkin.this.fromIndex + var2;
            }

            if (FXPaginationSkin.this.toIndex > FXPaginationSkin.this.getPageCount() - 1) {
                if (FXPaginationSkin.this.fromIndex > FXPaginationSkin.this.getPageCount() - 1) {
                    return false;
                }

                FXPaginationSkin.this.toIndex = FXPaginationSkin.this.getPageCount() - 1;
            }

            if (FXPaginationSkin.this.fromIndex < 0) {
                FXPaginationSkin.this.fromIndex = 0;
                FXPaginationSkin.this.toIndex = FXPaginationSkin.this.fromIndex + var2;
            }

            return true;
        }

        private int indexToIndicatorButtonsIndex(int idx) {
            if (idx >= FXPaginationSkin.this.fromIndex && idx <= FXPaginationSkin.this.toIndex) {
                return idx - FXPaginationSkin.this.fromIndex;
            } else {
                int var2 = 0;
                int fromIdx = FXPaginationSkin.this.fromIndex;
                int toIdx = FXPaginationSkin.this.toIndex;
                if (FXPaginationSkin.this.currentIndex > FXPaginationSkin.this.previousIndex) {
                    while (fromIdx < FXPaginationSkin.this.getPageCount() && toIdx < FXPaginationSkin.this.getPageCount()) {
                        fromIdx += var2;
                        toIdx += var2;
                        if (idx >= fromIdx && idx <= toIdx) {
                            if (idx == fromIdx) {
                                return 0;
                            }

                            if (idx == toIdx) {
                                return FXPaginationSkin.this.maxPageIndicatorCount - 1;
                            }
                            return idx - fromIdx;
                        }

                        var2 += FXPaginationSkin.this.maxPageIndicatorCount;
                    }
                } else {
                    while (fromIdx > 0 && toIdx > 0) {
                        fromIdx -= var2;
                        toIdx -= var2;
                        if (idx >= fromIdx && idx <= toIdx) {
                            if (idx == fromIdx) {
                                return 0;
                            }
                            if (idx == toIdx) {
                                return FXPaginationSkin.this.maxPageIndicatorCount - 1;
                            }
                            return idx - fromIdx;
                        }
                        var2 += FXPaginationSkin.this.maxPageIndicatorCount;
                    }
                }
                return FXPaginationSkin.this.maxPageIndicatorCount - 1;
            }
        }

        @Override
        protected double computeMinWidth(double var1) {
            double var3 = this.snappedLeftInset();
            double var5 = this.snappedRightInset();
            double var7 = this.snapSizeX(Utils.boundedSize(this.leftArrowButton.prefWidth(-1.0D), this.leftArrowButton.minWidth(-1.0D), this.leftArrowButton.maxWidth(-1.0D)));
            double var9 = this.snapSizeX(Utils.boundedSize(this.rightArrowButton.prefWidth(-1.0D), this.rightArrowButton.minWidth(-1.0D), this.rightArrowButton.maxWidth(-1.0D)));
            double var11 = this.snapSizeX(this.controlBox.getSpacing());

            double var16 = FXPaginationSkin.this.arrowButtonGap.get();
            return var3 + var7 + 2.0D * var16 + this.minButtonSize + 2.0D * var11 + var9 + var5;
        }

        @Override
        protected double computeMinHeight(double var1) {
            return this.computePrefHeight(var1);
        }

        @Override
        protected double computePrefWidth(double var1) {
            double var3 = this.snappedLeftInset();
            double var5 = this.snappedRightInset();
            double var7 = this.snapSizeX(this.controlBox.prefWidth(var1));

            return var3 + var7 + var5;
        }

        @Override
        protected double computePrefHeight(double var1) {
            double var3 = this.snappedTopInset();
            double var5 = this.snappedBottomInset();
            double controlHeight = this.snapSizeY(this.controlBox.prefHeight(var1));
            double infoHeight = this.snapSizeY(this.pageInformation.prefHeight(-1.0D));

            return var3 + controlHeight + infoHeight + var5;
        }

        @Override
        protected void layoutChildren() {
            double topInset = this.snappedTopInset();
            double bottomInset = this.snappedBottomInset();
            double leftInset = this.snappedLeftInset();
            double rightInset = this.snappedRightInset();
            double areaWidth = this.snapSizeX(this.getWidth()) - (leftInset + rightInset);
            double areaHeight = this.snapSizeY(this.getHeight()) - (topInset + bottomInset);
            double controlWidth = this.snapSizeX(this.controlBox.prefWidth(-1.0D));
            double controlHeight = this.snapSizeY(this.controlBox.prefHeight(-1.0D));
            double infoWidth = this.snapSizeX(this.pageInformation.prefWidth(-1.0D));
            double infoHeight = this.snapSizeY(this.pageInformation.prefHeight(-1.0D));
            this.leftArrowButton.setDisable(false);
            this.rightArrowButton.setDisable(false);
            if (FXPaginationSkin.this.currentIndex == 0) {
                this.leftArrowButton.setDisable(true);
            }

            if (FXPaginationSkin.this.currentIndex == FXPaginationSkin.this.getPageCount() - 1) {
                this.rightArrowButton.setDisable(true);
            }

            this.applyCss();
            this.leftArrowButton.setVisible(FXPaginationSkin.this.isArrowsVisible());
            this.rightArrowButton.setVisible(FXPaginationSkin.this.isArrowsVisible());
            this.pageInformation.setVisible(FXPaginationSkin.this.isPageInformationVisible());
            this.layoutPageIndicators();

            if (FXPaginationSkin.this.isPageInformationVisible()) {
                double controlY = areaHeight - infoHeight - bottomInset;
                double infoY = topInset + controlHeight;
                this.layoutInArea(this.controlBox, leftInset, topInset, areaWidth, controlHeight, 0, Insets.EMPTY, true, false, HPos.CENTER, VPos.TOP);
                this.layoutInArea(this.pageInformation, leftInset, infoY, areaWidth, infoHeight, 0, Insets.EMPTY, true, false, HPos.CENTER, VPos.TOP);
            } else {
                HPos hPos = this.controlBox.getAlignment().getHpos();
                VPos vPos = this.controlBox.getAlignment().getVpos();
                double controlX = leftInset + Utils.computeXOffset(areaWidth, controlWidth, hPos);
                double controlY = topInset + Utils.computeYOffset(areaHeight, controlHeight, vPos);
                this.layoutInArea(this.controlBox, controlX, controlY, controlWidth, controlHeight, 0.0D, hPos, vPos);
            }
        }
    }

    static class Utils {
        // used for layout to adjust widths to honor the min/max policies consistently
        public static double boundedSize(double value, double min, double max) {
            // if max < value, return max
            // if min > value, return min
            // if min > max, return min
            return Math.min(Math.max(value, min), Math.max(min, max));
        }

        public static double computeXOffset(double width, double contentWidth, HPos hPos) {
            if (hPos == null) {
                return 0.0D;
            } else {
                switch (hPos) {
                    case CENTER:
                        return (width - contentWidth) / 2.0D;
                    case RIGHT:
                        return width - contentWidth;
                    default:
                        return 0.0D;
                }
            }
        }

        public static double computeYOffset(double height, double contentHeight, VPos vPos) {
            if (vPos == null) {
                return 0.0D;
            } else {
                switch (vPos) {
                    case CENTER:
                        return (height - contentHeight) / 2.0D;
                    case BOTTOM:
                        return height - contentHeight;
                    default:
                        return 0.0D;
                }
            }
        }
    }

    class IndicatorButton extends ToggleButton {
        private final int pageNumber;

        public IndicatorButton(int pNum) {
            this.pageNumber = pNum;
            this.setFocusTraversable(false);
            this.setIndicatorType();

            this.setOnAction((var1x) -> {
                FXPaginationSkin.this.getNode().requestFocus();
                int currentPageIndex = FXPaginationSkin.this.getCurrentPageIndex();
                if (currentPageIndex != this.pageNumber) {
                    FXPaginationSkin.this.pagination.setCurrentPageIndex(this.pageNumber);
                    this.requestLayout();
                }
            });

            this.prefHeightProperty().bind(this.minHeightProperty());
            this.setAccessibleRole(AccessibleRole.PAGE_ITEM);
        }

        private void setIndicatorType() {
            this.getStyleClass().add("number-button");
            this.setText(Integer.toString(this.pageNumber + 1));
        }

        public int getPageNumber() {
            return this.pageNumber;
        }

        @Override
        public void fire() {
            if (this.getToggleGroup() == null || !this.isSelected()) {
                super.fire();
            }
        }

        public void release() {
        }

        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute var1, Object... var2) {
            switch (var1) {
                case TEXT:
                    return this.getText();
                case SELECTED:
                    return this.isSelected();
                default:
                    return super.queryAccessibleAttribute(var1, var2);
            }
        }

        @Override
        public void executeAccessibleAction(AccessibleAction var1, Object... var2) {
            switch (var1) {
                case REQUEST_FOCUS:
                    FXPaginationSkin.this.getSkinnable().setCurrentPageIndex(this.pageNumber);
                    break;
                default:
                    super.executeAccessibleAction(var1);
            }
        }
    }
}
