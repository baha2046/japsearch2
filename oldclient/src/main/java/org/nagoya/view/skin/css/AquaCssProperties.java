package org.nagoya.view.skin.css;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.PaintConverter;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.nagoya.view.skin.AquaButtonSkin;
import org.nagoya.view.skin.AquaFocusBorder;
import org.nagoya.view.skin.styles.MacOSDefaultIconConverter;
import org.nagoya.view.skin.styles.MacOSDefaultIcons;

public class AquaCssProperties {
    private static CssMetaData<Button, MacOSDefaultIcons> iconMetadata;
    private static CssMetaData<Control, Paint> innerFocusColorMetadata;
    private static CssMetaData<Control, Paint> outerFocusColorMetadata;

    public AquaCssProperties() {
    }

    public static <T> StyleableObjectProperty<T> createProperty(final Object bean, final String propertyName, final CssMetaData<? extends Styleable, T> metaData) {
        return new StyleableObjectProperty<T>() {
            @Override
            public CssMetaData<? extends Styleable, T> getCssMetaData() {
                return metaData;
            }

            @Override
            public Object getBean() {
                return bean;
            }

            @Override
            public String getName() {
                return propertyName;
            }
        };
    }

    public static CssMetaData<Button, MacOSDefaultIcons> getIconMetaData() {
        if (iconMetadata == null) {
            iconMetadata = new CssMetaData<Button, MacOSDefaultIcons>("-fx-aqua-icon", MacOSDefaultIconConverter.getInstance()) {
                @Override
                public boolean isSettable(Button n) {
                    Skin<?> skin = n.getSkin();
                    if (skin instanceof AquaButtonSkin) {
                        return ((AquaButtonSkin) skin).iconProperty() == null || !((AquaButtonSkin) skin).iconProperty().isBound();
                    } else {
                        return false;
                    }
                }

                @Override
                public StyleableProperty<MacOSDefaultIcons> getStyleableProperty(Button n) {
                    Skin<?> skin = n.getSkin();
                    return skin instanceof AquaButtonSkin ? (StyleableProperty) ((AquaButtonSkin) skin).iconProperty() : null;
                }
            };
        }

        return iconMetadata;
    }

    public static CssMetaData<Control, Paint> getInnerFocusColorMetaData() {
        if (innerFocusColorMetadata == null) {
            innerFocusColorMetadata = new CssMetaData<Control, Paint>("-fx-aqua-inner-focus-color", PaintConverter.getInstance(), Color.BLUE) {
                @Override
                public boolean isSettable(Control n) {
                    Skin<?> skin = n.getSkin();
                    return skin instanceof AquaFocusBorder;
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(Control n) {
                    Skin<?> skin = n.getSkin();
                    return skin instanceof AquaFocusBorder ? (StyleableProperty) ((AquaFocusBorder) skin).innerFocusColorProperty() : null;
                }
            };
        }

        return innerFocusColorMetadata;
    }

    public static CssMetaData<Control, Paint> getOuterFocusColorMetaData() {
        if (outerFocusColorMetadata == null) {
            outerFocusColorMetadata = new CssMetaData<Control, Paint>("-fx-aqua-outer-focus-color", PaintConverter.getInstance(), Color.BLUE) {
                @Override
                public boolean isSettable(Control n) {
                    Skin<?> skin = n.getSkin();
                    return skin instanceof AquaFocusBorder;
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(Control n) {
                    Skin<?> skin = n.getSkin();
                    return skin instanceof AquaFocusBorder ? (StyleableProperty) ((AquaFocusBorder) skin).outerFocusColorProperty() : null;
                }
            };
        }

        return outerFocusColorMetadata;
    }
}
