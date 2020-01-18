package org.nagoya.view.skin;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.BooleanConverter;
import javafx.scene.Cursor;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.nagoya.view.skin.css.AquaCssProperties;
import org.nagoya.view.skin.effects.FocusBorder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AquaTextFieldSkin extends TextFieldSkin implements AquaSkin, AquaFocusBorder {
    private Region searchIconPath;
    private Region cancelSearchIconPath;
    private FocusBorder focusBorder;
    private BooleanProperty showSearchIcon;
    private ObjectProperty<Paint> innerFocusColor;
    private ObjectProperty<Paint> outerFocusColor;

    private final InvalidationListener focusChanged = observable -> this.onFocusChanged();

    public AquaTextFieldSkin(TextField textfield) {
        super(textfield);
        //this.registerChangeListener(textfield.focusedProperty(), "FOCUSED");
        textfield.focusedProperty().addListener(this.focusChanged);

        if (this.getSkinnable().isFocused()) {
            this.setFocusBorder();
        }

        this.showSearchIconProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != oldValue) {
                if (newValue) {
                    if (this.searchIconPath == null) {
                        this.searchIconPath = new Region();
                        this.searchIconPath.getStyleClass().add("icon-search");
                    }

                    this.getChildren().add(this.searchIconPath);
                } else if (oldValue != null && this.searchIconPath != null) {
                    this.getChildren().remove(this.searchIconPath);
                }

                this.getSkinnable().requestLayout();
            }

        });

        this.showSearchIconProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue) {
                    if (this.cancelSearchIconPath == null) {
                        this.cancelSearchIconPath = new Region();
                        this.cancelSearchIconPath.getStyleClass().add("icon-delete");
                        this.cancelSearchIconPath.setOnMouseClicked(event ->
                                AquaTextFieldSkin.this.getSkinnable().setText(""));
                    }

                    this.getSkinnable().textProperty().addListener((observable1, oldValue1, newValue1) -> {
                        if (newValue1 != null && newValue1.length() != 0) {
                            this.cancelSearchIconPath.setVisible(true);
                            this.cancelSearchIconPath.setCursor(Cursor.DEFAULT);
                        } else {
                            this.cancelSearchIconPath.setVisible(false);
                        }

                    });
                    if (this.getSkinnable().getText() == null || this.getSkinnable().getText().length() == 0) {
                        this.cancelSearchIconPath.setVisible(false);
                    }

                    this.getChildren().add(this.cancelSearchIconPath);
                } else if (this.cancelSearchIconPath != null) {
                    this.getChildren().remove(this.cancelSearchIconPath);
                }

                this.getSkinnable().requestLayout();
            }

        });
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        double height;
        if (this.searchIconPath != null) {
            this.searchIconPath.toFront();
            height = this.getSkinnable().getHeight();
            this.searchIconPath.setLayoutX(0.0D);
            this.searchIconPath.setLayoutY(0.0D);
            this.searchIconPath.resize(height, height);
        }

        if (this.cancelSearchIconPath != null) {
            this.cancelSearchIconPath.toFront();
            height = this.getSkinnable().getHeight();
            this.cancelSearchIconPath.setTranslateX(this.getSkinnable().getWidth() - height * 1.5D);
            this.cancelSearchIconPath.setLayoutY(height * 0.15D);
            this.cancelSearchIconPath.resize(height * 0.55D, height * 0.55D);
        }

    }

    public FocusBorder getFocusBorder() {
        if (this.focusBorder == null) {
            this.focusBorder = new FocusBorder();
        }

        return this.focusBorder;
    }

    private void setFocusBorder() {
        this.getFocusBorder().setInnerFocusColor((Color) this.innerFocusColorProperty().get());
        this.getFocusBorder().setColor((Color) this.outerFocusColorProperty().get());
        this.getSkinnable().setEffect(this.getFocusBorder());
    }

    protected void onFocusChanged() {
        if (!(this.getSkinnable().getParent() instanceof ComboBox)) {
            if (this.getSkinnable().isFocused()) {
                this.setFocusBorder();
            } else {
                this.getSkinnable().setEffect(null);
            }
        }
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> ret = new ArrayList<>(super.getCssMetaData());
        ret.addAll(getClassCssMetaData());
        return ret;
    }

    public final BooleanProperty showSearchIconProperty() {
        if (this.showSearchIcon == null) {
            this.showSearchIcon = new StyleableBooleanProperty() {
                @Override
                public CssMetaData<? extends Styleable, Boolean> getCssMetaData() {
                    return StyleableProperties.SHOW_SEARCH_ICON;
                }

                @Override
                public Object getBean() {
                    return this;
                }

                @Override
                public String getName() {
                    return "showSearchIcon";
                }
            };
        }

        return this.showSearchIcon;
    }

    public void setShowSearchIcon(Boolean showSearchIcon) {
        this.showSearchIconProperty().setValue(showSearchIcon);
    }

    public boolean isShowSearchIcon() {
        return this.showSearchIcon == null ? false : this.showSearchIcon.getValue();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    @Override
    public final ObjectProperty<Paint> innerFocusColorProperty() {
        if (this.innerFocusColor == null) {
            this.innerFocusColor = AquaCssProperties.createProperty(this, "innerFocusColor", AquaCssProperties.getInnerFocusColorMetaData());
        }

        return this.innerFocusColor;
    }

    @Override
    public void setInnerFocusColor(Paint innerFocusColor) {
        this.innerFocusColorProperty().setValue(innerFocusColor);
    }

    @Override
    public Paint getInnerFocusColor() {
        return this.innerFocusColor == null ? null : this.innerFocusColor.getValue();
    }

    @Override
    public final ObjectProperty<Paint> outerFocusColorProperty() {
        if (this.outerFocusColor == null) {
            this.outerFocusColor = AquaCssProperties.createProperty(this, "outerFocusColor", AquaCssProperties.getOuterFocusColorMetaData());
        }

        return this.outerFocusColor;
    }

    @Override
    public void setOuterFocusColor(Paint outerFocusColor) {
        this.outerFocusColorProperty().setValue(outerFocusColor);
    }

    @Override
    public Paint getOuterFocusColor() {
        return this.outerFocusColor == null ? null : this.outerFocusColor.getValue();
    }

    private static class StyleableProperties {
        private static final CssMetaData<TextField, Boolean> SHOW_SEARCH_ICON = new CssMetaData<TextField, Boolean>("-fx-show-search-icon", BooleanConverter.getInstance(), false) {
            @Override
            public boolean isSettable(TextField n) {
                Skin<?> skin = n.getSkin();
                if (skin instanceof AquaTextFieldSkin) {
                    return ((AquaTextFieldSkin) skin).showSearchIcon == null || !((AquaTextFieldSkin) skin).showSearchIcon.isBound();
                } else {
                    return false;
                }
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(TextField n) {
                Skin<?> skin = n.getSkin();
                return skin instanceof AquaTextFieldSkin ? (StyleableProperty) ((AquaTextFieldSkin) skin).showSearchIconProperty() : null;
            }
        };
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        private StyleableProperties() {
        }

        static {
            List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Control.getClassCssMetaData());
            styleables.add(SHOW_SEARCH_ICON);
            styleables.add(AquaCssProperties.getInnerFocusColorMetaData());
            styleables.add(AquaCssProperties.getOuterFocusColorMetaData());
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
}
