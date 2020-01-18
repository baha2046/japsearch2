package org.nagoya.controls;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import org.nagoya.view.skin.FXPaginationSkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class FXPagination extends Control {
    private static final int DEFAULT_MAX_PAGE_INDICATOR_COUNT = 10;
    private static final String DEFAULT_STYLE_CLASS = "pagination";

    private int oldMaxPageIndicatorCount;
    private IntegerProperty maxPageIndicatorCount;
    private int oldPageCount;
    private final IntegerProperty pageCount;
    private final IntegerProperty currentPageIndex;
    private final ObjectProperty<Consumer<Integer>> pageFactory;

    public FXPagination(int maxCount) {
        this.oldMaxPageIndicatorCount = DEFAULT_MAX_PAGE_INDICATOR_COUNT;
        this.oldPageCount = 2147483647;
        this.pageCount = new SimpleIntegerProperty(this, "pageCount", 2147483647) {
            @Override
            protected void invalidated() {
                if (!FXPagination.this.pageCount.isBound()) {
                    if (FXPagination.this.getPageCount() < 1) {
                        FXPagination.this.setPageCount(FXPagination.this.oldPageCount);
                    }
                    FXPagination.this.oldPageCount = FXPagination.this.getPageCount();
                }
            }
        };
        this.currentPageIndex = new SimpleIntegerProperty(this, "currentPageIndex", 0) {
            @Override
            protected void invalidated() {
                // GUICommon.debugMessage("SET currentPageIndex " + this.get());
                if (!FXPagination.this.currentPageIndex.isBound()) {
                    if (FXPagination.this.getCurrentPageIndex() < 0) {
                        FXPagination.this.setCurrentPageIndex(0);
                    } else if (FXPagination.this.getCurrentPageIndex() > FXPagination.this.getPageCount() - 1) {
                        FXPagination.this.setCurrentPageIndex(FXPagination.this.getPageCount() - 1);
                    }
                }
            }

            @Override
            public void bind(ObservableValue<? extends Number> var1) {
                throw new UnsupportedOperationException("currentPageIndex supports only bidirectional binding");
            }
        };
        this.pageFactory = new SimpleObjectProperty<>(this, "pageFactory");
        this.getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        this.setAccessibleRole(AccessibleRole.PAGINATION);
        this.setMaxPageIndicatorCount(maxCount);
        this.setPageCount(0);
        this.setCurrentPageIndex(0);
    }


    public final IntegerProperty maxPageIndicatorCountProperty() {
        if (this.maxPageIndicatorCount == null) {
            this.maxPageIndicatorCount = new StyleableIntegerProperty(10) {
                @Override
                protected void invalidated() {
                    if (!FXPagination.this.maxPageIndicatorCount.isBound()) {
                        if (FXPagination.this.getMaxPageIndicatorCount() < 1) {
                            FXPagination.this.setMaxPageIndicatorCount(FXPagination.this.oldMaxPageIndicatorCount);
                        }
                        FXPagination.this.oldMaxPageIndicatorCount = FXPagination.this.getMaxPageIndicatorCount();
                    }
                }

                @Override
                public CssMetaData<FXPagination, Number> getCssMetaData() {
                    return FXPagination.StyleableProperties.MAX_PAGE_INDICATOR_COUNT;
                }

                @Override
                public Object getBean() {
                    return FXPagination.this;
                }

                @Override
                public String getName() {
                    return "maxPageIndicatorCount";
                }
            };
        }

        return this.maxPageIndicatorCount;
    }

    public final void setMaxPageIndicatorCount(int var1) {
        this.maxPageIndicatorCountProperty().set(var1);
    }

    public final int getMaxPageIndicatorCount() {
        return this.maxPageIndicatorCount == null ? 10 : this.maxPageIndicatorCount.get();
    }

    public final void setPageCount(int var1) {
        this.pageCount.set(var1);
    }

    public final int getPageCount() {
        return this.pageCount.get();
    }

    public final IntegerProperty pageCountProperty() {
        return this.pageCount;
    }

    public final void setCurrentPageIndex(int var1) {
        this.currentPageIndex.set(var1);
    }

    public final int getCurrentPageIndex() {
        return this.currentPageIndex.get();
    }

    public final IntegerProperty currentPageIndexProperty() {
        return this.currentPageIndex;
    }

    public final void setPageFactory(Consumer<Integer> var1) {
        this.pageFactory.set(var1);
    }

    public final Consumer<Integer> getPageFactory() {
        return this.pageFactory.get();
    }

    public final ObjectProperty<Consumer<Integer>> pageFactoryProperty() {
        return this.pageFactory;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new FXPaginationSkin(this);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return FXPagination.StyleableProperties.STYLEABLES;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    private static class StyleableProperties {
        private static final CssMetaData<FXPagination, Number> MAX_PAGE_INDICATOR_COUNT = new CssMetaData<>("-fx-max-page-indicator-count", SizeConverter.getInstance(), 10) {
            @Override
            public boolean isSettable(FXPagination var1) {
                return var1.maxPageIndicatorCount == null || !var1.maxPageIndicatorCount.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(FXPagination var1) {
                return (StyleableProperty<Number>) var1.maxPageIndicatorCountProperty();
            }
        };
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        private StyleableProperties() {
        }

        static {
            ArrayList<CssMetaData<? extends Styleable, ?>> var0 = new ArrayList<>(Control.getClassCssMetaData());
            var0.add(MAX_PAGE_INDICATOR_COUNT);
            STYLEABLES = Collections.unmodifiableList(var0);
        }
    }
}
