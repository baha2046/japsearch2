package org.nagoya.view.skin;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.skin.ButtonSkin;
import org.nagoya.view.skin.css.AquaCssProperties;
import org.nagoya.view.skin.styles.MacOSDefaultIcons;

public class AquaButtonSkin extends ButtonSkin {
    private ObjectProperty<MacOSDefaultIcons> icon;

    public AquaButtonSkin(Button button) {
        super(button);
    }

    public final ObjectProperty<MacOSDefaultIcons> iconProperty() {
        if (this.icon == null) {
            this.icon = AquaCssProperties.createProperty(this, "icon", AquaCssProperties.getIconMetaData());
        }

        return this.icon;
    }
}
