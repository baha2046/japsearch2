package org.nagoya.view.skin.styles;

public enum MacOSDefaultIcons implements StyleDefinition {
    LEFT,
    RIGHT,
    SHARE,
    SEARCH,
    DELETE;

    private MacOSDefaultIcons() {
    }

    @Override
    public String getStyleName() {
        String prefix = "icon";
        if (this.equals(LEFT)) {
            return prefix + "-" + "left";
        } else if (this.equals(RIGHT)) {
            return prefix + "-" + "right";
        } else if (this.equals(SHARE)) {
            return prefix + "-" + "share";
        } else if (this.equals(SEARCH)) {
            return prefix + "-" + "search";
        } else {
            return this.equals(DELETE) ? prefix + "-" + "delete" : null;
        }
    }
}