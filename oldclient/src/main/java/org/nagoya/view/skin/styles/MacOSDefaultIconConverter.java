package org.nagoya.view.skin.styles;

import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;

public final class MacOSDefaultIconConverter extends StyleConverter<String, MacOSDefaultIcons> {
    public static StyleConverter<String, MacOSDefaultIcons> getInstance() {
        return MacOSDefaultIconConverter.Holder.ICON_INSTANCE;
    }

    private MacOSDefaultIconConverter() {
    }

    @Override
    public MacOSDefaultIcons convert(ParsedValue<String, MacOSDefaultIcons> value, Font font) {
        String str = (String) value.getValue();
        if (str != null && !str.isEmpty() && !"null".equals(str)) {
            try {
                return MacOSDefaultIcons.valueOf(str);
            } catch (IllegalArgumentException var5) {
                System.err.println("not a Mac Icon: " + value);
                return MacOSDefaultIcons.RIGHT;
            }
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "MacOSDefaultIconConverter";
    }

    private static class Holder {
        static MacOSDefaultIconConverter ICON_INSTANCE = new MacOSDefaultIconConverter();

        private Holder() {
        }
    }
}

